package com.program.bookie.server;

import com.program.bookie.db.DatabaseConnection;
import com.program.bookie.models.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private DatabaseConnection dbManager;
    private User currentUser;
    private String clientAddress;

    public ClientHandler(Socket socket, DatabaseConnection dbManager) {
        this.clientSocket = socket;
        this.dbManager = dbManager;
        this.clientAddress = socket.getRemoteSocketAddress().toString();
        System.out.println("New client connected: " + clientAddress);
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());

            while (!clientSocket.isClosed()) {
                try {
                    Request request = (Request) input.readObject();


                    if (request.getType() == RequestType.DISCONNECT) {
                        System.out.println("Client " + clientAddress + " requested disconnect");
                        break;
                    }

                    Response response = handleRequest(request);
                    output.writeObject(response);
                    output.flush();
                } catch (SocketException e) {
                    System.out.println("Client " + clientAddress + " disconnected unexpectedly");
                    break;
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
                        System.out.println("Client " + clientAddress + " connection reset");
                    } else {
                        System.out.println("Client " + clientAddress + " disconnected: " + e.getMessage());
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("Error reading request from " + clientAddress + ": " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error initializing connection with " + clientAddress + ": " + e.getMessage());
        } finally {
            System.out.println("Closing connection for client: " + clientAddress);
            closeConnection();
        }}

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
            case GET_USER_RATING:
                return handleGetUserRating(request);
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
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String username = (String) data.get("username");
            Integer bookId = (Integer) data.get("bookId");

            if (username == null || bookId == null) {
                return new Response(ResponseType.ERROR, "Missing username or bookId");
            }

            String status = dbManager.getStatus(username, bookId);
            return new Response(ResponseType.SUCCESS, status);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetReadingStatus: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetReadingStatus: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleUpdateReadingStatus(Request request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String username = (String) data.get("username");
            Integer bookId = (Integer) data.get("bookId");
            String status = (String) data.get("status");

            if (username == null || bookId == null || status == null) {
                return new Response(ResponseType.ERROR, "Missing username, bookId, or status");
            }

            dbManager.updateStatus(username, bookId, status);
            return new Response(ResponseType.SUCCESS, "Reading status updated successfully");

        } catch (SQLException e) {
            System.err.println("Database error in handleUpdateReadingStatus: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleUpdateReadingStatus: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleGetUserRating(Request request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String username = (String) data.get("username");
            Integer bookId = (Integer) data.get("bookId");

            if (username == null || bookId == null) {
                return new Response(ResponseType.ERROR, "Missing username or bookId");
            }

            Integer rating = dbManager.getUserRating(username, bookId);
            return new Response(ResponseType.SUCCESS, rating);

        } catch (SQLException e) {
            System.err.println("Blad bazy danych handleGetUserRating: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Blad bazy danych: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Wyjatek podczas handleGetUserRating: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleGetUserReview(Request request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String username = (String) data.get("username");
            Integer bookId = (Integer) data.get("bookId");

            if (username == null || bookId == null) {
                return new Response(ResponseType.ERROR, "Missing username or bookId");
            }

            Review review = dbManager.getUserReview(username, bookId);
            return new Response(ResponseType.SUCCESS, review);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetUserReview: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetUserReview: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleSaveUserReview(Request request) {
        try {
            Review review = (Review) request.getData();

            if (review.getUsername() == null || review.getBookId() == 0) {
                return new Response(ResponseType.ERROR, "Missing username or bookId");
            }

            if (review.getRating() < 1 || review.getRating() > 5) {
                return new Response(ResponseType.ERROR, "Rating must be between 1 and 5");
            }

            dbManager.saveUserReview(
                    review.getUsername(),
                    review.getBookId(),
                    review.getRating(),
                    review.getReviewText()
            );

            return new Response(ResponseType.SUCCESS, "Review saved successfully");

        } catch (SQLException e) {
            System.err.println("Blad bazy danych podczas handleSaveUserReview: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database blad: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Blad przy handleSaveUserReview: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error : " + e.getMessage());
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
