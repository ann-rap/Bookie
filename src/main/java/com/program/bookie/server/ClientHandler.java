package com.program.bookie.server;

import com.program.bookie.db.DatabaseConnection;
import com.program.bookie.models.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private DatabaseConnection dbManager;
    private User currentUser;

    public ClientHandler(Socket socket, DatabaseConnection dbManager) {
        this.clientSocket = socket;
        this.dbManager = dbManager;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());

            // Główna pętla obsługi żądań
            while (!clientSocket.isClosed()) {
                Request request = (Request) input.readObject();
                Response response = handleRequest(request);
                output.writeObject(response);
                output.flush();
            }
        } catch (Exception e) {
            System.err.println("Błąd obsługi klienta: " + e.getMessage());
        } finally {

            closeConnection();
        }
    }

    private Response handleRequest(Request request) {
        switch (request.getType()) {
            case LOGIN:
                return handleLogin(request);
            case REGISTER:
                return handleRegister(request);
            case GET_TOP_BOOKS:
                return handleGetTopRatedBooks(request);
            case SEARCH_BOOK:
                return handleSearch(request);
            case GET_READING_STATUS:
                return handleGetReadingStatus(request);
            case UPDATE_READING_STATUS:
                return handleUpdateReadingStatus(request);
            default:
                return new Response(ResponseType.ERROR, "Nieznany typ żądania");
        }
    }

    private Response handleLogin(Request request) {
        LoginData loginData = (LoginData) request.getData();

        try {
            User user = dbManager.authenticateUser(loginData.getUsername(), loginData.getPassword());
            if (user != null) {
                currentUser = user;
                return new Response(ResponseType.SUCCESS, user);
            } else {
                return new Response(ResponseType.ERROR, "Nieprawidłowe dane logowania");
            }
        } catch (SQLException e) {
            return new Response(ResponseType.ERROR, "Błąd bazy danych: " + e.getMessage());
        }
    }

    private Response handleRegister(Request request) {
        RegisterData registerData = (RegisterData) request.getData();

        try {
            ResponseType result = dbManager.registerUser(
                    registerData.getFirstname(),
                    registerData.getLastname(),
                    registerData.getUsername(),
                    registerData.getPassword()
            );

            if (result == ResponseType.SUCCESS) {
                // Optionally authenticate newly registered user
                User user = dbManager.authenticateUser(registerData.getUsername(), registerData.getPassword());
                return new Response(ResponseType.SUCCESS, user);
            } else if (result == ResponseType.INFO) {
                return new Response(ResponseType.ERROR, "Username already exists");
            } else {
                return new Response(ResponseType.ERROR, "Registration failed");
            }

        } catch (SQLException e) {
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        }
    }

    private Response handleGetTopRatedBooks(Request request) {
        try {
            Integer limit = (Integer) request.getData();
            if (limit == null) {
                limit = 4; // Domyślnie 4 książki
            }

            List<Book> topBooks = dbManager.getTopRatedBooks(limit);
            return new Response(ResponseType.SUCCESS, topBooks);
        } catch (SQLException e) {
            return new Response(ResponseType.ERROR, "Błąd bazy danych: " + e.getMessage());
        }
    }

    private Response handleSearch(Request request) {
        try {
            String title = (String) request.getData();
            List<Book> topBooks = dbManager.searchByTitle(title);
            return new Response(ResponseType.SUCCESS, topBooks);
        } catch (SQLException e) {
            return new Response(ResponseType.ERROR, "Błąd bazy danych: " + e.getMessage());
        }
    }

    private Response handleGetReadingStatus(Request request) {
        try {
            Map<String, Object> data = (Map<String, Object>) request.getData();
            int userId = (Integer) data.get("userId");
            int bookId = (Integer) data.get("bookId");

            String status = dbManager.getStatus(currentUser.getUsername(), bookId);
            return new Response(ResponseType.SUCCESS, status);
        } catch (SQLException e) {
            return new Response(ResponseType.ERROR, "Błąd bazy danych: " + e.getMessage());
        }
    }

    private Response handleUpdateReadingStatus(Request request) {
        try {
            Map<String, Object> data = (Map<String, Object>) request.getData();
            int userId = (Integer) data.get("userId");
            int bookId = (Integer) data.get("bookId");
            String status = (String) data.get("status");

            dbManager.updateStatus(currentUser.getUsername(), bookId, status);
            return new Response(ResponseType.SUCCESS, "Status zaktualizowany");
        } catch (SQLException e) {
            return new Response(ResponseType.ERROR, "Błąd bazy danych: " + e.getMessage());
        }
    }

    private void closeConnection(){
        try{
            if(input!=null) input.close();
            if(output!=null) output.close();
            if(clientSocket!=null) clientSocket.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

    }
}
