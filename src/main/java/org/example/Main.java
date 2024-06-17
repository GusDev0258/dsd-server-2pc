package org.example;

import org.example.model.Coordinator;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            new Coordinator().startServer();
        } catch (IOException exception) {
            System.out.println("Erro na inicialização do servidor" + exception.getMessage());
        }
    }
}