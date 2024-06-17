package org.example.model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class Coordinator {
    private List<ParticipantHandler> participants = new ArrayList<>();

    private String internalDecision = "";
    private ServerSocket server;

    private String currentState = "";

    public void startServer() throws IOException {
        try {
            this.server = new ServerSocket(5222);
        } catch (IOException exception) {
            System.out.println("Não foi possível iniciar o servidor na porta 5222" + exception.getMessage());
        }
        System.out.println("Servidor do coordenador iniciado");
        this.currentState = CoordinatorState.INIT;
        System.out.println("Estado atual: " + this.currentState);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(() -> {
            System.out.println("Tempo de espera para participantes terminou.");
            try {
                this.server.close();
            } catch (IOException exception) {
                System.out.println("Problemas no servidor" + exception.getMessage());
            }
        }, 1, TimeUnit.MINUTES);

        try {
            while (!this.server.isClosed()) {
                Socket socket = this.server.accept();
                ParticipantHandler participantHandler = new ParticipantHandler(socket);
                participants.add(participantHandler);
                new Thread(participantHandler).start();
                System.out.println("Participante conectado.");
            }
        } catch (IOException exception) {
            System.out.println("Parando de aceitar novas conexões.");
        }

        executor.shutdown();
        if (!participants.isEmpty()) {
            this.start2PC();
        } else {
            System.out.println("Nenhum participante conectado. Abortando transação.");
        }
    }

    public void start2PC() {

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        List<Future<String>> futures = new ArrayList<>();
        ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(executor);

        for (ParticipantHandler participant : participants) {
            participant.sendMessage(Message.REQUEST);
            futures.add(completionService.submit(participant::readMessage));
        }
        this.currentState = CoordinatorState.WAIT;
        System.out.println("Estado atual: " + this.currentState);

        executor.schedule(() -> {
            for (Future<String> future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        }, 1, TimeUnit.MINUTES);

        boolean allYes = true;

        for (int i = 0; i < participants.size(); i++) {
            try {
                Future<String> future = completionService.poll(1, TimeUnit.MINUTES);
                if (future == null || !Message.COMMIT.equals(future.get())) {
                    allYes = false;
                    break;
                }
            } catch (InterruptedException | ExecutionException exception) {
                allYes = false;
                System.out.println("Exceção na comunicação com um participante: " + exception.getMessage());
                break;
            }
        }

        if (allYes) {
            this.currentState = CoordinatorState.READY;
            System.out.println("Estado atual: " + this.currentState);
            for (ParticipantHandler participant : participants) {
                participant.sendMessage(Message.GLOBAL_COMMIT);
                this.internalDecision = Message.GLOBAL_COMMIT;
            }
            this.currentState = CoordinatorState.COMMIT;
            System.out.println("Estado atual: " + this.currentState);
            System.out.println("Transação Confirmada.");
        } else {
            this.currentState = CoordinatorState.ABORT;
            System.out.println("Estado atual: " + this.currentState);
            for (ParticipantHandler participant : participants) {
                participant.sendMessage(Message.GLOBAL_ABORT);
                this.internalDecision = Message.GLOBAL_ABORT;
            }
            System.out.println("Transação Abortada.");
        }
        executor.shutdown();
    }
}
