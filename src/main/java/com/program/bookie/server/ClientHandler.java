package com.program.bookie.server;

import com.program.bookie.db.DatabaseConnection;
import com.program.bookie.models.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import com.program.bookie.models.UserStatistics;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import com.program.bookie.models.ReadingInsights;
import com.program.bookie.models.ImageData;
import java.io.FileNotFoundException;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private DatabaseConnection dbManager;
    private ImageService imageService;
    private User currentUser;
    private String clientAddress;

    public ClientHandler(Socket socket, DatabaseConnection dbManager) {
        this.clientSocket = socket;
        this.dbManager = dbManager;
        this.imageService = new ImageService();
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
            case GET_IMAGE:
                return handleGetImage(request);
            case GET_USER_REVIEW:
                return handleGetUserReview(request);
            case SAVE_USER_REVIEW:
                return handleSaveUserReview(request);
            case GET_RANDOM_QUOTE:
                return handleGetRandomQuote(request);
            case GET_USER_STATISTICS:
                return handleGetUserStatistics(request);
            case GET_READING_INSIGHTS:
                return handleGetReadingInsights(request);
            case GET_BOOK_REVIEWS:
                return handleGetBookReviews(request);
            case GET_REVIEW_COMMENTS:
                return handleGetReviewComments(request);
            case ADD_COMMENT:
                return handleAddComment(request);
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
                return new Response(ResponseType.ERROR, "Invalid username or password");
            }
        } catch (SQLException e) {
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
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
                return new Response(ResponseType.ERROR, "Brak uzytkownika lub id ksiazki");
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
                    review.getReviewText(),
                    review.isSpoiler()
            );

            return new Response(ResponseType.SUCCESS, "Review saved successfully");

        } catch (SQLException e) {
            System.err.println("Database error in handleSaveUserReview: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleSaveUserReview: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleGetRandomQuote(Request request) {
        try {
            Quote randomQuote = dbManager.getRandomQuote();
            return new Response(ResponseType.SUCCESS, randomQuote);
        } catch (Exception e) {
            System.err.println("Error getting random quote: " + e.getMessage());
            // Zwróć domyślny cytat w przypadku błędu
            Quote defaultQuote = new Quote("Welcome to Bookie - your personal reading companion!", "Bookie Team");
            return new Response(ResponseType.SUCCESS, defaultQuote);
        }
    }

    private Response handleGetUserStatistics(Request request) {
        try {
            String username = (String) request.getData();

            if (username == null || username.trim().isEmpty()) {
                return new Response(ResponseType.ERROR, "Username is required");
            }

            UserStatistics stats = dbManager.getUserStatistics(username);
            return new Response(ResponseType.SUCCESS, stats);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetUserStatistics: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetUserStatistics: " + e.getMessage());
                 return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleGetBookReviews(Request request) {
        try {
            Integer bookId = (Integer) request.getData();

            if (bookId == null) {
                return new Response(ResponseType.ERROR, "Book ID is required");
            }

            List<Review> reviews = dbManager.getBookReviews(bookId);
            return new Response(ResponseType.SUCCESS, reviews);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetBookReviews: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetBookReviews: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleGetReadingInsights(Request request) {
        try {
            String username = (String) request.getData();

            if (username == null || username.trim().isEmpty()) {
                return new Response(ResponseType.ERROR, "Username is required");
            }

            ReadingInsights insights = dbManager.getReadingInsights(username);
            return new Response(ResponseType.SUCCESS, insights);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetReadingInsights: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetReadingInsights: " + e.getMessage());
                   return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }
    private Response handleGetReviewComments(Request request) {
        try {
            Integer reviewId = (Integer) request.getData();

            if (reviewId == null) {
                return new Response(ResponseType.ERROR, "Review ID is required");
            }

            List<Comment> comments = dbManager.getReviewComments(reviewId);
            return new Response(ResponseType.SUCCESS, comments);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetReviewComments: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetReviewComments: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleAddComment(Request request) {
        try {
            Comment comment = (Comment) request.getData();

            if (comment.getUsername() == null || comment.getContent() == null) {
                return new Response(ResponseType.ERROR, "Username and content are required");
            }

            if (comment.getReviewId() == 0) {
                return new Response(ResponseType.ERROR, "Review ID is required");
            }

            // Sprawdź czy review istnieje
            if (!dbManager.reviewExists(comment.getReviewId())) {
                return new Response(ResponseType.ERROR, "Review not found");
            }

            dbManager.addComment(comment.getUsername(), comment.getReviewId(), comment.getContent());
            return new Response(ResponseType.SUCCESS, "Comment added successfully");

        } catch (SQLException e) {
            System.err.println("Database error in handleAddComment: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleAddComment: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
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

    private Response handleGetImage(Request request) {
        try {
            String filename = (String) request.getData();

            if (filename == null || filename.trim().isEmpty()) {
                return new Response(ResponseType.ERROR, "Filename is required");
            }

            ImageData imageData = imageService.getImage(filename);
            return new Response(ResponseType.SUCCESS, imageData);

        } catch (FileNotFoundException e) {
            return new Response(ResponseType.ERROR, "Image not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error loading image: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error in handleGetImage: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Unexpected error: " + e.getMessage());
        }
    }

}

