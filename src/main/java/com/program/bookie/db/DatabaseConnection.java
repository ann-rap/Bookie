package com.program.bookie.db;

import com.program.bookie.models.User;
import com.program.bookie.models.*;
import com.program.bookie.server.QuoteService;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bookie";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private QuoteService quoteService;

    public DatabaseConnection() throws SQLException {
        this.connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
        this.quoteService = new QuoteService();
    }

    /*
    Zatwierdzenie logowania, zwraca uzytkownika jesli sie uda.
     */
    public User authenticateUser(String username, String password) throws SQLException {
        String query = "SELECT * FROM user_account WHERE username = ? AND password = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("account_id"),
                        rs.getString("firstname"),
                        rs.getString("lastname"),
                        rs.getString("username")
                );
            }
        }
        return null;
    }

    /*
    Sprawdzenie czy dany uzytkownik juz nie istnieje. Uzywane przy rejestracji.
     */
    public boolean userExists(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_account WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    /*
    Rejestracja uzytkownika, przyjmuje juz sprawdzone dane. Wstawia dane do bazy danych. Zwraca czy sie udalo czy jakis problem.
     */
    public ResponseType registerUser(String firstname, String lastname, String username, String password) throws SQLException {
        if (userExists(username)) {
            return ResponseType.INFO;
        }

        String query = "INSERT INTO user_account (firstname, lastname, username, password) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, firstname);
            stmt.setString(2, lastname);
            stmt.setString(3, username);
            stmt.setString(4, password);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                return ResponseType.SUCCESS;
            } else {
                return ResponseType.ERROR;
            }
        }
    }

    /*
    N najlepiej ocenianych ksiazek w postaci listy.
    Uzywane przy poleceniach na stronie glownej.
     */
    public List<Book> getTopRatedBooks(int limit) throws SQLException {
        String query = """
        SELECT b.book_id, b.title, b.author, b.cover_image, 
               COALESCE(AVG(r.rating), 0) as avg_rating,
               COUNT(r.rating) as rating_count,
               b.description, b.genre, b.publication_year
        FROM books b 
        LEFT JOIN reviews r ON b.book_id = r.book_id 
        GROUP BY b.book_id, b.title, b.author, b.cover_image, 
                 b.description, b.genre, b.publication_year
        ORDER BY avg_rating DESC, rating_count DESC 
        LIMIT ?
        """;

        List<Book> topBooks = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Book book = new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("cover_image"),
                        rs.getDouble("avg_rating"),
                        rs.getInt("rating_count"),
                        rs.getString("description"),
                        rs.getString("genre"),
                        rs.getInt("publication_year")
                );
                topBooks.add(book);
            }
        }

        return topBooks;
    }

    /*
    Informacje o ksiazce na podstawie id. Bedzie potrzebne do szczegolow o ksiazce.
     */
    public Book getBookById(int bookId) throws SQLException {
        String query = """
        SELECT b.book_id, b.title, b.author, b.cover_image, 
               COALESCE(AVG(r.rating), 0) as avg_rating,
               COUNT(r.rating) as rating_count,
               b.description, b.genre, b.publication_year
        FROM books b 
        LEFT JOIN reviews r ON b.book_id = r.book_id 
        WHERE b.book_id = ?
        GROUP BY b.book_id, b.title, b.author, b.cover_image, 
                 b.description, b.genre, b.publication_year
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Book(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("cover_image"),
                        rs.getDouble("avg_rating"),
                        rs.getInt("rating_count"),
                        rs.getString("description"),
                        rs.getString("genre"),
                        rs.getInt("publication_year")
                );
            }
        }

        return null;}

    /*
    Metoda ktora zwraca liste obiektow Ksiazka, po czesci tytulu.
    Bierze z bazy danych i od razu oblicza avg_rating i ile opinii.
    Uzywana przy wyszukiwaniu po nazwie.
     */
    public List<Book> searchByTitle(String title) throws SQLException {

        String sql= """
        SELECT b.book_id, b.title, b.author, b.cover_image, 
               COALESCE(AVG(r.rating), 0) as avg_rating,
               COUNT(r.rating) as rating_count,
               b.description, b.genre, b.publication_year
        FROM books b 
        LEFT JOIN reviews r ON b.book_id = r.book_id 
        WHERE title LIKE ?
        GROUP BY b.book_id, b.title, b.author, b.cover_image, 
                 b.description, b.genre, b.publication_year ORDER BY avg_rating DESC 
""";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, "%" + title + "%");
        ResultSet rs = stmt.executeQuery();

        List<Book> results = new ArrayList<>();
        while (rs.next()) {
            results.add(new Book(
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("cover_image"),
                    rs.getDouble("avg_rating"),
                    rs.getInt("rating_count"),
                    rs.getString("description"),
                    rs.getString("genre"),
                    rs.getInt("publication_year")
            ));
        }
        return results;
    }

    public String getStatus(String username, int bookId) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String sql = """
    SELECT bu.reading_status
    FROM user_books bu
    INNER JOIN user_account ua ON bu.user_id = ua.account_id
    WHERE ua.username = ? AND bu.book_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String enumValue = rs.getString("reading_status");
                    return convertToDisplayValue(enumValue);
                } else {
                    return null;
                }
            }
        }
    }



    /*
    Zaktualizuj lub wstaw status
    */
    public void updateStatus(String username, int bookId, String newStatus) throws SQLException {
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }


        String enumStatus = convertToEnumValue(newStatus);


        LocalDate currentDate = LocalDate.now();

        String sqlCheck = """
    SELECT 1 FROM user_books bu
    INNER JOIN user_account ua ON bu.user_id = ua.account_id
    WHERE ua.username = ? AND bu.book_id = ?
    """;

        String sqlUpdate = """
    UPDATE user_books bu
    INNER JOIN user_account ua ON bu.user_id = ua.account_id
    SET bu.reading_status = ?, bu.date_added = ?
    WHERE ua.username = ? AND bu.book_id = ?
    """;

        String sqlInsert = """
    INSERT INTO user_books (user_id, book_id, reading_status, date_added)
    SELECT ua.account_id, ?, ?, ?
    FROM user_account ua
    WHERE ua.username = ?
    """;

        try (PreparedStatement checkStmt = connection.prepareStatement(sqlCheck)) {
            checkStmt.setString(1, username.trim());
            checkStmt.setInt(2, bookId);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement updateStmt = connection.prepareStatement(sqlUpdate)) {
                        updateStmt.setString(1, enumStatus);
                        updateStmt.setDate(2, java.sql.Date.valueOf(currentDate));
                        updateStmt.setString(3, username.trim());
                        updateStmt.setInt(4, bookId);
                        int rowsUpdated = updateStmt.executeUpdate();
                        if (rowsUpdated == 0) {
                            throw new SQLException("Failed to update reading status");
                        }
                    }
                } else {

                    try (PreparedStatement insertStmt = connection.prepareStatement(sqlInsert)) {
                        insertStmt.setInt(1, bookId);
                        insertStmt.setString(2, enumStatus);
                        insertStmt.setDate(3, java.sql.Date.valueOf(currentDate));
                        insertStmt.setString(4, username.trim());
                        int rowsInserted = insertStmt.executeUpdate();
                        if (rowsInserted == 0) {
                            throw new SQLException("Failed to insert reading status");
                        }
                    }
                }
            }
        }
    }


    private String convertToDisplayValue(String enumValue) {
        if (enumValue == null) return null;
        switch (enumValue) {
            case "TO_READ":
                return "Want to read";
            case "READING":
                return "Currently reading";
            case "READ":
                return "Read";
            default:
                return enumValue; // fallback
        }
    }

    private String convertToEnumValue(String displayValue) {
        if (displayValue == null) return null;
        switch (displayValue) {
            case "Want to read":
                return "TO_READ";
            case "Currently reading":
                return "READING";
            case "Read":
                return "READ";
            default:
                throw new IllegalArgumentException("Unknown status: " + displayValue);
        }
    }

    public Integer getUserRating(String username, int bookId) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String sql = """
    SELECT r.rating
    FROM reviews r
    INNER JOIN user_account ua ON r.user_id = ua.account_id
    WHERE ua.username = ? AND r.book_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rating");
                } else {
                    return null;
                }
            }
        }
    }
    /*
    Pobierz review użytkownika dla konkretnej książki
    */
    public Review getUserReview(String username, int bookId) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String sql = """
    SELECT r.review_id, r.user_id, r.book_id, r.rating, r.content, r.is_spoiler
    FROM reviews r
    INNER JOIN user_account ua ON r.user_id = ua.account_id
    WHERE ua.username = ? AND r.book_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Review review = new Review();
                    review.setReviewId(rs.getInt("review_id"));
                    review.setUserId(rs.getInt("user_id"));
                    review.setBookId(rs.getInt("book_id"));
                    review.setRating(rs.getInt("rating"));
                    review.setReviewText(rs.getString("content"));
                    review.setSpoiler(rs.getBoolean("is_spoiler"));
                    review.setUsername(username);
                    return review;
                } else {
                    return null;
                }
            }
        }
    }

    /*
    Zapisz lub zaktualizuj review użytkownika
    */
    public void saveUserReview(String username, int bookId, int rating, String reviewText, boolean isSpoiler) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Logika spoilerów: zapisz jako spoiler tylko jeśli jest tekst I isSpoiler = true
        boolean finalIsSpoiler = (reviewText != null && !reviewText.trim().isEmpty()) && isSpoiler;

        // Sprawdź czy review już istnieje
        String sqlCheck = """
    SELECT r.review_id FROM reviews r
    INNER JOIN user_account ua ON r.user_id = ua.account_id
    WHERE ua.username = ? AND r.book_id = ?
    """;

        String sqlUpdate = """
    UPDATE reviews r
    INNER JOIN user_account ua ON r.user_id = ua.account_id
    SET r.rating = ?, r.content = ?, r.is_spoiler = ?, r.updated_at = CURRENT_TIMESTAMP
    WHERE ua.username = ? AND r.book_id = ?
    """;

        String sqlInsert = """
    INSERT INTO reviews (user_id, book_id, rating, content, is_spoiler, created_at, updated_at)
    SELECT ua.account_id, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM user_account ua
    WHERE ua.username = ?
    """;

        try (PreparedStatement checkStmt = connection.prepareStatement(sqlCheck)) {
            checkStmt.setString(1, username.trim());
            checkStmt.setInt(2, bookId);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Review istnieje - aktualizuj
                    try (PreparedStatement updateStmt = connection.prepareStatement(sqlUpdate)) {
                        updateStmt.setInt(1, rating);
                        updateStmt.setString(2, reviewText);
                        updateStmt.setBoolean(3, finalIsSpoiler);
                        updateStmt.setString(4, username.trim());
                        updateStmt.setInt(5, bookId);

                        int rowsUpdated = updateStmt.executeUpdate();
                        if (rowsUpdated == 0) {
                            throw new SQLException("Failed to update review");
                        }
                    }
                } else {
                    // Review nie istnieje - wstaw nowy
                    try (PreparedStatement insertStmt = connection.prepareStatement(sqlInsert)) {
                        insertStmt.setInt(1, bookId);
                        insertStmt.setInt(2, rating);
                        insertStmt.setString(3, reviewText);
                        insertStmt.setBoolean(4, finalIsSpoiler);
                        insertStmt.setString(5, username.trim());

                        int rowsInserted = insertStmt.executeUpdate();
                        if (rowsInserted == 0) {
                            throw new SQLException("Failed to insert review");
                        }
                    }
                }
            }
        }
    }

    /*
    Pobiera losowy cytat o książkach
    */
    public Quote getRandomQuote() {
        return quoteService.getRandomQuote();
    }


    //Zamykanie polaczenia
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Błąd zamykania połączenia: " + e.getMessage());
        }
    }
}