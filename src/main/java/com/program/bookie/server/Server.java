package com.program.bookie.server;
import com.program.bookie.db.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Scanner;
import java.util.logging.*;

public class Server implements Runnable {
    private static final Logger logger = Logger.getLogger("ServerLogger");

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private DatabaseConnection dbManager;

    private final ServerConfig config = ServerConfig.getInstance();
    private final int PORT;


    public Server() {
        this.PORT = config.getServerPort();
    }

    public void run() {
        setupLogger();

        try {
            dbManager = new DatabaseConnection();
            serverSocket = new ServerSocket(PORT);
            running = true;

            logger.info("SERVER_START - Server started on port: " + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, dbManager);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            logger.severe("SERVER_ERROR - " + e.getMessage());
        } catch (SQLException e) {
            logger.severe("DB_CONNECTION_ERROR - " + e.getMessage());
        }
    }

    private void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setFormatter(new CustomFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            System.err.println("Failed to setup logger: " + e.getMessage());
        }
    }

    // Custom formatter z timestampami
    private static class CustomFormatter extends Formatter {
        private static final DateTimeFormatter TIMESTAMP_FORMAT =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public String format(LogRecord record) {
            return String.format("[%s] [%s] %s%n",
                    LocalDateTime.now().format(TIMESTAMP_FORMAT),
                    record.getLevel(),
                    record.getMessage()
            );
        }
    }


    public void stop() {
        logger.info("SERVER_STOP - Shutting down server");
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            dbManager.closeConnection();
        } catch (IOException e) {
            logger.info("SERVER_SHUTDOWN_ERROR - " + e.getMessage());
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