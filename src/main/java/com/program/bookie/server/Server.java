package com.program.bookie.server;
import com.program.bookie.db.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Scanner;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private final int PORT = 999;
    private volatile boolean running = false;
    private DatabaseConnection dbManager;

    public void run() {
        try {
            dbManager = new DatabaseConnection();
            serverSocket = new ServerSocket(PORT);
            running = true;

            System.out.println("Serwer uruchomiony na porcie: " + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, dbManager);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Błąd serwera: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            dbManager.closeConnection();
        } catch (IOException e) {
            System.err.println("Błąd podczas zamykania serwera: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        new Thread(server).start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            if (command.equalsIgnoreCase("close")) {
                server.stop();
            }

        }
    }

}