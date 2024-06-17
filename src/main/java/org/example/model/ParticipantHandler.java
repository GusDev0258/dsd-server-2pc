package org.example.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ParticipantHandler implements Runnable{
    private Socket socket;
    private BufferedReader inReader;
    private PrintWriter outWriter;

    public ParticipantHandler(Socket socket) throws IOException{
        this.socket = socket;
        this.inReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.outWriter = new PrintWriter(socket.getOutputStream(), true);
    }

    public void sendMessage(String message) {
       outWriter.println(message);
    }

    public String readMessage() throws IOException {
        return inReader.readLine();
    }

    @Override
    public void run() {

    }
}
