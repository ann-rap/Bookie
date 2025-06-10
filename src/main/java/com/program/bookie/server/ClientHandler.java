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
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger("ServerLogger");

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
        logger.info("CLIENT_CONNECTED - " + clientAddress);
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
                        logger.info("CLIENT_DISCONNECTED - " + clientAddress);
                        break;
                    }

                    Response response = handleRequest(request);
                    output.writeObject(response);
                    output.flush();
                } catch (SocketException e) {
                    logger.info("CLIENT_DISCONNECTED - " + clientAddress);
                    break;

                } catch (ClassNotFoundException e) {
                    logger.warning("CLIENT_ERROR - " + clientAddress + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.warning("CLIENT_CONNECTION_ERROR - " + clientAddress + ": " + e.getMessage());
        } finally {
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
            case GET_USER_BOOKS_BY_STATUS:
                return handleGetUserBooksByStatus(request);
            case GET_BOOK_WITH_PROGRESS:
                return handleGetBookWithProgress(request);
            case UPDATE_PROGRESS:
                return handleUpdateProgress(request);
            case GET_BOOK_PROGRESS:
                return handleGetBookProgress(request);
            case GET_NOTIFICATION_COUNT:
                return handleGetNotificationCount(request);
            case GET_NOTIFICATIONS:
                return handleGetNotifications(request);
            case MARK_NOTIFICATIONS_READ:
                return handleMarkNotificationsRead(request);
            case CLEAR_NOTIFICATIONS:
                return handleClearNotifications(request);
            case GET_BOOK_BY_ID:
                return handleGetBookById(request);
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
                logger.info("LOGIN_SUCCESS - User: " + loginData.getUsername() + ", Client: " + clientAddress);
                return new Response(ResponseType.SUCCESS, user);
            } else {
                logger.warning("LOGIN_FAILED - User: " + loginData.getUsername() + ", Client: " + clientAddress);
                return new Response(ResponseType.ERROR, "Invalid username or password");
            }
        } catch (SQLException e) {
            logger.severe("LOGIN_DB_ERROR - " + e.getMessage());
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
                logger.info("REGISTER_SUCCESS - User: " + registerData.getUsername() + ", Client: " + clientAddress);
                User user = dbManager.authenticateUser(registerData.getUsername(), registerData.getPassword());
                return new Response(ResponseType.SUCCESS, user);
            } else if (result == ResponseType.INFO) {
                return new Response(ResponseType.ERROR, "Username already exists");
            } else {
                logger.warning("REGISTER_FAILED - User: " + registerData.getUsername());
                return new Response(ResponseType.ERROR, "Registration failed");
            }

        } catch (SQLException e) {
            logger.severe("REGISTER_DB_ERROR - " + e.getMessage());
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

    private Response handleGetBookProgress(Request request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String username = (String) data.get("username");
            Integer bookId = (Integer) data.get("bookId");

            if (username == null || bookId == null) {
                return new Response(ResponseType.ERROR, "Missing username or bookId");
            }

            Book bookWithProgress = dbManager.getBookWithProgress(username, bookId);
            return new Response(ResponseType.SUCCESS, bookWithProgress);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetBookProgress: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetBookProgress: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }}


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

            logger.info("STATUS_UPDATE - User: " + username + ", Book: " + bookId + ", Status: " + status);
            return new Response(ResponseType.SUCCESS, "Reading status updated successfully");

        } catch (SQLException e) {
            logger.severe("STATUS_UPDATE_ERROR - " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
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
            String reviewText = review.getReviewText() != null && !review.getReviewText().trim().isEmpty()
                    ? "with_text" : "rating_only";
            logger.info("REVIEW_SAVE - User: " + review.getUsername() +
                    ", Book: " + review.getBookId() +
                    ", Rating: " + review.getRating() +
                    ", Type: " + reviewText);

            return new Response(ResponseType.SUCCESS, "Review saved successfully");

        } catch (SQLException e) {
            logger.severe("REVIEW_SAVE_ERROR - " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
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

            dbManager.addCommentWithNotification(comment.getUsername(), comment.getReviewId(), comment.getContent());
            logger.info("COMMENT_ADD - User: "+comment.getUsername()+" Review: "+comment.getReviewId()+" Comment: "+comment.getContent());
            return new Response(ResponseType.SUCCESS, "Comment added with notification");

        } catch (SQLException e) {
            logger.severe("COMMENT_ADD_ERROR - " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
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
     private Response handleGetNotificationCount(Request request) {
        try {
            String username = (String) request.getData();

            if (username == null || username.trim().isEmpty()) {
                return new Response(ResponseType.ERROR, "Username is required");
            }

            // Get user ID from username
            int userId = getUserIdFromUsername(username);
            int count = dbManager.getUnreadNotificationCount(userId);

            return new Response(ResponseType.SUCCESS, count);

        } catch (Exception e) {
            System.err.println("Error getting notification count: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error: " + e.getMessage());
        }
    }
    private Response handleGetNotifications(Request request) {
        try{
            @SuppressWarnings("unchecked")
            Map<String,Object> data = (Map<String, Object>) request.getData();
            String username=(String)data.get("username");
            Boolean unreadOnly = (Boolean) data.get("unreadOnly");

            if (username == null || username.trim().isEmpty()) {
                return new Response(ResponseType.ERROR, "Username is required");
            }

            int userId = getUserIdFromUsername(username);
            List<INotification> notifications = dbManager.getUserNotifications(userId,
                    unreadOnly != null ? unreadOnly : false);

            return new Response(ResponseType.SUCCESS, notifications);

        } catch (Exception e) {
            System.err.println("Error getting notifications: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error: " + e.getMessage());
        }
    }

private Response handleMarkNotificationsRead(Request request) {
    try{
        @SuppressWarnings("unchecked")
        Map<String,Object> data = (Map<String, Object>) request.getData();
        String username=(String)data.get("username");
        List<Integer> notificationIds = (List<Integer>) data.get("notificationIds");

            if (username == null || notificationIds == null) {
                return new Response(ResponseType.ERROR, "Username and notification IDs required");
            }

            int userId = getUserIdFromUsername(username);
            dbManager.markNotificationsAsRead(userId, notificationIds);

            return new Response(ResponseType.SUCCESS, "Notifications marked as read");

        } catch (Exception e) {
            System.err.println("Error marking notifications read: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error: " + e.getMessage());
        }
    }

    private Response handleGetUserBooksByStatus(Request request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String username = (String) data.get("username");
            String status = (String) data.get("status");

            System.out.println("handleGetUserBooksByStatus - username: " + username + ", status: " + status);

            if (username == null || status == null) {
                return new Response(ResponseType.ERROR, "Missing username or status");
            }

            List<Book> userBooks = dbManager.getUserBooksByStatus(username, status);
            System.out.println("Found " + userBooks.size() + " books for status: " + status);
            return new Response(ResponseType.SUCCESS, userBooks);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetUserBooksByStatus: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetUserBooksByStatus: " + e.getMessage());
            e.printStackTrace();
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleGetBookWithProgress(Request request) {

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String username = (String) data.get("username");
            Integer bookId = (Integer) data.get("bookId");

            if (username == null || bookId == null) {
                return new Response(ResponseType.ERROR, "Missing username or bookId");
            }

            Book bookWithProgress = dbManager.getBookWithProgress(username, bookId);
            return new Response(ResponseType.SUCCESS, bookWithProgress);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetBookWithProgress: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetBookWithProgress: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }

    private Response handleUpdateProgress(Request request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String username = (String) data.get("username");
            Integer bookId = (Integer) data.get("bookId");
            Integer currentPage = (Integer) data.get("currentPage");

            if (username == null || bookId == null || currentPage == null) {
                return new Response(ResponseType.ERROR, "Missing username, bookId, or currentPage");
            }

            if (currentPage < 0) {
                return new Response(ResponseType.ERROR, "Invalid page number");
            }

            dbManager.updateBookProgress(username, bookId, currentPage);
            return new Response(ResponseType.SUCCESS, "Progress updated successfully");

        } catch (SQLException e) {
            System.err.println("Database error in handleUpdateProgress: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleUpdateProgress: " + e.getMessage());
        return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());}
    }
        

    private Response handleClearNotifications(Request request) {
        try {
            String username = (String) request.getData();

            if (username == null || username.trim().isEmpty()) {
                return new Response(ResponseType.ERROR, "Username is required");
            }

            int userId = getUserIdFromUsername(username);
            dbManager.clearUserNotifications(userId);
            logger.info("NOTIFICATIONS_CLEAR - User: " + username);

            return new Response(ResponseType.SUCCESS, "Notifications cleared");

        } catch (Exception e) {
            logger.severe("NOTIFICATIONS_CLEAR_ERROR - " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error: " + e.getMessage());
        }
    }

    private Response handleGetBookById(Request request) {
        try {
            Integer bookId = (Integer) request.getData();

            if (bookId == null) {
                return new Response(ResponseType.ERROR, "Book ID is required");
            }

            Book book = dbManager.getBookById(bookId);
            return new Response(ResponseType.SUCCESS, book);

        } catch (SQLException e) {
            System.err.println("Database error in handleGetBookById: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleGetBookById: " + e.getMessage());
            return new Response(ResponseType.ERROR, "Error processing request: " + e.getMessage());
        }
    }


    private int getUserIdFromUsername(String username) throws SQLException {
        return dbManager.getUserIdByUsername(username);
    }

}

