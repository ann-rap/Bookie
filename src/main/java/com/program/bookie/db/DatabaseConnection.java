package com.program.bookie.db;

import com.program.bookie.models.User;
import com.program.bookie.models.*;
import com.program.bookie.server.QuoteService;

import java.sql.*;
import java.time.LocalDate;
import com.program.bookie.models.UserStatistics;

import java.util.*;

import com.program.bookie.models.ReadingInsights;


public class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bookie";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "hasloProgram";

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
        String query = "SELECT * FROM user_account WHERE BINARY username = ? AND BINARY password = ?";

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

        String query = "INSERT INTO user_account (firstname, lastname, username, password, account_created_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
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
               COUNT(CASE WHEN r.content IS NOT NULL AND TRIM(r.content) != '' THEN 1 END) as review_count,
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
                book.setReviewCount(rs.getInt("review_count"));
                topBooks.add(book);
            }
        }

        return topBooks;
    }



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
               COUNT(CASE WHEN r.content IS NOT NULL AND TRIM(r.content) != '' THEN 1 END) as review_count,        
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

            book.setReviewCount(rs.getInt("review_count"));

            results.add(book);

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

    /**
     * Pobiera statystyki uzytkownika
     */
    public UserStatistics getUserStatistics(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String userInfoSql = """
        SELECT account_created_date, account_id
        FROM user_account 
        WHERE username = ?
        """;

        LocalDate accountCreatedDate = LocalDate.now();
        int userId = -1;

        try (PreparedStatement stmt = connection.prepareStatement(userInfoSql)) {
            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp createdTimestamp = rs.getTimestamp("account_created_date");
                    accountCreatedDate = createdTimestamp != null ?
                            createdTimestamp.toLocalDateTime().toLocalDate() : LocalDate.now();
                    userId = rs.getInt("account_id");
                } else {
                    return new UserStatistics(0, 0, 0, 0, 0, 0.0, LocalDate.now());
                }
            }
        }

        String bookStatsSql = """
        SELECT 
            reading_status,
            COUNT(*) as count
        FROM user_books 
        WHERE user_id = ?
        GROUP BY reading_status
        """;

        int booksRead = 0;
        int booksCurrentlyReading = 0;
        int booksWantToRead = 0;

        try (PreparedStatement stmt = connection.prepareStatement(bookStatsSql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("reading_status");
                    int count = rs.getInt("count");

                    switch (status) {
                        case "READ":
                            booksRead = count;
                            break;
                        case "READING":
                            booksCurrentlyReading = count;
                            break;
                        case "TO_READ":
                            booksWantToRead = count;
                            break;
                    }
                }
            }
        }

        String reviewStatsSql = """
    SELECT 
        COUNT(*) as reviews_written,
        COALESCE(AVG(rating), 0) as average_rating
    FROM reviews 
    WHERE user_id = ? 
    AND content IS NOT NULL 
    AND TRIM(content) != ''
    """;
        int reviewsWritten = 0;
        double averageRating = 0.0;

        try (PreparedStatement stmt = connection.prepareStatement(reviewStatsSql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    reviewsWritten = rs.getInt("reviews_written");
                    averageRating = rs.getDouble("average_rating");
                }
            }
        }
        String pagesSql = """
    SELECT COALESCE(SUM(b.pages), 0) as total_pages
    FROM user_books ub
    INNER JOIN books b ON ub.book_id = b.book_id
    WHERE ub.user_id = ? AND ub.reading_status = 'READ'
    """;

        int totalPagesRead = 0;
        try (PreparedStatement stmt = connection.prepareStatement(pagesSql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalPagesRead = rs.getInt("total_pages");
                }
            }
        }

        return new UserStatistics(
                booksRead,
                booksCurrentlyReading,
                booksWantToRead,
                totalPagesRead,
                reviewsWritten,
                averageRating,
                accountCreatedDate
        );
    }

    /**
     * Pobiera książkę z najwyższą oceną użytkownika
     */
    public Book getUserHighestRatedBook(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String sql = """
    SELECT b.book_id, b.title, b.author, b.cover_image, 
           COALESCE(AVG(r2.rating), 0) as avg_rating,
           COUNT(r2.rating) as rating_count,
           b.description, b.genre, b.publication_year,
           r.rating as user_rating
    FROM reviews r
    INNER JOIN user_account ua ON r.user_id = ua.account_id
    INNER JOIN books b ON r.book_id = b.book_id
    LEFT JOIN reviews r2 ON b.book_id = r2.book_id
    WHERE ua.username = ?
    GROUP BY b.book_id, b.title, b.author, b.cover_image, 
             b.description, b.genre, b.publication_year, r.rating
    ORDER BY r.rating DESC, b.title ASC
    LIMIT 1
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
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
        }

        return null;
    }

    /**
     * Pobiera ocenę użytkownika dla jego najwyżej ocenionej książki
     */
    public Integer getUserHighestRating(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String sql = """
    SELECT MAX(r.rating) as highest_rating
    FROM reviews r
    INNER JOIN user_account ua ON r.user_id = ua.account_id
    WHERE ua.username = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("highest_rating");
                }
            }
        }

        return null;
    }

    /**
     * Pobiera książkę z największą liczbą recenzji (Most Popular)
     */
    public Book getMostPopularBook() throws SQLException {
        String sql = """
    SELECT b.book_id, b.title, b.author, b.cover_image, 
           COALESCE(AVG(r.rating), 0) as avg_rating,
           COUNT(r.rating) as rating_count,
           COUNT(CASE WHEN r.content IS NOT NULL AND TRIM(r.content) != '' THEN 1 END) as review_count,
           b.description, b.genre, b.publication_year
    FROM books b 
    LEFT JOIN reviews r ON b.book_id = r.book_id 
    GROUP BY b.book_id, b.title, b.author, b.cover_image, 
             b.description, b.genre, b.publication_year
    HAVING COUNT(r.rating) > 0
    ORDER BY rating_count DESC, avg_rating DESC
    LIMIT 1
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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

                    book.setReviewCount(rs.getInt("review_count"));

                    return book;
                }
            }
        }

        return null;
    }

    /**
     * Pobiera książkę z najwyższą średnią oceną (Community Favorite)
     */
    public Book getCommunityFavoriteBook() throws SQLException {
        String sql = """
    SELECT b.book_id, b.title, b.author, b.cover_image, 
           COALESCE(AVG(r.rating), 0) as avg_rating,
           COUNT(r.rating) as rating_count,
           b.description, b.genre, b.publication_year
    FROM books b 
    LEFT JOIN reviews r ON b.book_id = r.book_id 
    GROUP BY b.book_id, b.title, b.author, b.cover_image, 
             b.description, b.genre, b.publication_year
    HAVING COUNT(r.rating) >= 3
    ORDER BY avg_rating DESC, rating_count DESC
    LIMIT 1
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
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
        }

        return null;
    }

    public ReadingInsights getReadingInsights(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        ReadingInsights insights = new ReadingInsights();

        insights.setUserHighestRatedBook(getUserHighestRatedBook(username));
        insights.setUserHighestRating(getUserHighestRating(username));

        insights.setMostPopularBook(getMostPopularBook());

        insights.setCommunityFavoriteBook(getCommunityFavoriteBook());

        return insights;
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

    /**
     * Aktualizuje ścieżkę do okładki książki
     */
    public void updateBookCover(int bookId, String coverImagePath) throws SQLException {
        String sql = "UPDATE books SET cover_image = ? WHERE book_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, coverImagePath);
            stmt.setInt(2, bookId);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("No book found with ID: " + bookId);
            }

            System.out.println("Updated cover for book ID " + bookId + " to: " + coverImagePath);
        }
    }

    /**
     * Pobiera ścieżkę do okładki książki
     */
    public String getBookCoverPath(int bookId) throws SQLException {
        String sql = "SELECT cover_image FROM books WHERE book_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("cover_image");
                } else {
                    throw new SQLException("No book found with ID: " + bookId);
                }
            }
        }
    }

    /**
     * Sprawdza czy książka istnieje
     */
    public boolean bookExists(int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM books WHERE book_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    /**
     * Pobiera wszystkie reviews dla danej książki wraz z datami
     */
    public List<Review> getBookReviews(int bookId) throws SQLException {
        String sql = """
        SELECT r.review_id, r.user_id, r.book_id, r.rating, r.content, r.is_spoiler,
               r.created_at, r.updated_at, ua.username
        FROM reviews r
        INNER JOIN user_account ua ON r.user_id = ua.account_id
                        WHERE r.book_id = ? AND r.content IS NOT NULL
        AND TRIM(r.content) != ''
        ORDER BY r.created_at DESC
        """;

        List<Review> reviews = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setReviewId(rs.getInt("review_id"));
                    review.setUserId(rs.getInt("user_id"));
                    review.setBookId(rs.getInt("book_id"));
                    review.setRating(rs.getInt("rating"));
                    review.setReviewText(rs.getString("content"));
                    review.setSpoiler(rs.getBoolean("is_spoiler"));
                    review.setUsername(rs.getString("username"));

                    // Konwersja Timestamp na LocalDateTime
                    if (rs.getTimestamp("created_at") != null) {
                        review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    if (rs.getTimestamp("updated_at") != null) {
                        review.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    }

                    reviews.add(review);
                }
            }
        }

        return reviews;
    }

    /**
     * Pobiera komentarze dla danego review
     */
    public List<Comment> getReviewComments(int reviewId) throws SQLException {
        String sql = """
        SELECT c.comment_id, c.review_id, c.user_id, c.content, c.created_at,
               ua.username
        FROM comments c
        INNER JOIN user_account ua ON c.user_id = ua.account_id
        WHERE c.review_id = ?
        ORDER BY c.created_at ASC
        """;

        List<Comment> comments = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reviewId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Comment comment = new Comment(
                            rs.getInt("comment_id"),
                            rs.getInt("review_id"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    comments.add(comment);
                }
            }
        }

        return comments;
    }

    /**
     * Dodaje nowy komentarz
     */
    public void addComment(String username, int reviewId, String content) throws SQLException {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }

        String sql = """
        INSERT INTO comments (review_id, user_id, content, created_at)
        SELECT ?, ua.account_id, ?, CURRENT_TIMESTAMP
        FROM user_account ua
        WHERE ua.username = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reviewId);
            stmt.setString(2, content.trim());
            stmt.setString(3, username);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted == 0) {
                throw new SQLException("Failed to insert comment - user not found: " + username);
            }
        }
    }

    /**
     * Pobiera liczbę komentarzy dla danego review
     */
    public int getCommentsCount(int reviewId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM comments WHERE review_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reviewId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }

    /**
     * Sprawdza czy review istnieje
     */
    public boolean reviewExists(int reviewId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews WHERE review_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reviewId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    /**
     * Adds a new notification to the database
     */
    public int addNotification(INotification notification, Map<String, String> additionalData) throws SQLException {
        String sql = """
        INSERT INTO notifications (user_id, notification_type, title, message, related_id, is_read)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, notification.getUserId());
            stmt.setString(2, notification.getNotificationType());
            stmt.setString(3, notification.getTitle());
            stmt.setString(4, notification.getMessage());

            if (notification.getRelatedId() != null) {
                stmt.setInt(5, notification.getRelatedId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setBoolean(6, notification.isRead());

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int notificationId = rs.getInt(1);

                        // Store additional data if provided
                        if (additionalData != null && !additionalData.isEmpty()) {
                            storeNotificationData(notificationId, additionalData);
                        }

                        return notificationId;
                    }
                }
            }
        }
        throw new SQLException("Failed to insert notification");
    }

    //POWIADOMIENIA

    /**
     * Dodatkowe informacje dla notyfikacji
     */
    private void storeNotificationData(int notificationId, Map<String, String> data) throws SQLException {
        String sql = "INSERT INTO notification_data (notification_id, data_key, data_value) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                stmt.setInt(1, notificationId);
                stmt.setString(2, entry.getKey());
                stmt.setString(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Zwraca iczbe nieprzeczytanych, do aktualizacji czerwonego koleczka
     */
    public int getUnreadNotificationCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public Integer getUserIdByUsername(String username) throws SQLException {
        String sql = "SELECT account_id FROM user_account WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("account_id");
                } else {
                    throw new SQLException("User not found: " + username);
                }
            }
        }
    }

    /**
     * Wszystkie powiadomieni uzytkownika
     */
    public List<INotification> getUserNotifications(int userId, boolean unreadOnly) throws SQLException {
        String sql = """
        SELECT n.*, 
               nd1.data_value as commenter_name,
               nd2.data_value as book_title,
               nd3.data_value as book_id
        FROM notifications n
        LEFT JOIN notification_data nd1 ON n.notification_id = nd1.notification_id AND nd1.data_key = 'commenter_name'
        LEFT JOIN notification_data nd2 ON n.notification_id = nd2.notification_id AND nd2.data_key = 'book_title'
        LEFT JOIN notification_data nd3 ON n.notification_id = nd3.notification_id AND nd3.data_key = 'book_id'
        WHERE n.user_id = ?
        """;

        if (unreadOnly) {
            sql += " AND n.is_read = FALSE";
        }
        sql += " ORDER BY n.created_at DESC LIMIT 50";

        List<INotification> notifications = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    INotification notification = buildNotificationFromResultSet(rs);
                    if (notification != null) {
                        notifications.add(notification);
                    }
                }
            }
        }

        return notifications;
    }

    /**
     * Tworzenie odpowiedneigo typu powiadomienia
     */
    private INotification buildNotificationFromResultSet(ResultSet rs) throws SQLException {
        String type = rs.getString("notification_type");
        INotification notification;

        switch (type) {
            case "COMMENT_REPLY":
                CommentReplyNotification commentNotif = new CommentReplyNotification();
                commentNotif.setCommenterName(rs.getString("commenter_name"));
                commentNotif.setBookTitle(rs.getString("book_title"));
                commentNotif.setReviewId(rs.getInt("related_id"));
                notification = commentNotif;
                break;

            case "READING_REMINDER":
                ReadingReminderNotification reminderNotif = new ReadingReminderNotification();
                String bookTitle = rs.getString("book_title");
                if (bookTitle != null) {
                    reminderNotif.setBookTitle(bookTitle);
                    reminderNotif.setBookId(rs.getInt("book_id"));
                    reminderNotif.setReminderType(ReadingReminderNotification.ReminderType.SPECIFIC_BOOK);
                } else {
                    reminderNotif.setReminderType(ReadingReminderNotification.ReminderType.GENERAL_READING);
                }
                notification = reminderNotif;
                break;

            default:
                return null;
        }

        notification.setNotificationId(rs.getInt("notification_id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        return notification;
    }

    /**
     * Oznaczenie powiadomienia jako przeczytanego
     */
    public void markNotificationsAsRead(int userId, List<Integer> notificationIds) throws SQLException {
        if (notificationIds.isEmpty()) return;

        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND notification_id IN (" +
                String.join(",", Collections.nCopies(notificationIds.size(), "?")) + ")";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            int index = 2;
            for (Integer id : notificationIds) {
                stmt.setInt(index++, id);
            }
            stmt.executeUpdate();
        }
    }

    /**
     * Wyczyszczenie powiadomien z tabeli
     */
    public void clearUserNotifications(int userId) throws SQLException {
        String sql = "DELETE FROM notifications WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Tworzenie powiadomienia przy dodaniu komentarza
     */
    public void addCommentWithNotification(String username, int reviewId, String content) throws SQLException {

        addComment(username, reviewId, content);


        String sql = """
        SELECT r.user_id, b.title, ua.username
        FROM reviews r
        INNER JOIN books b ON r.book_id = b.book_id
        INNER JOIN user_account ua ON ua.username = ?
        WHERE r.review_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, reviewId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int reviewOwnerId = rs.getInt("user_id");
                    String bookTitle = rs.getString("title");
                    String commenterName = rs.getString("username");

                    // Don't notify if commenting on own review
                    if (!username.equals(commenterName)) {
                        CommentReplyNotification notification = NotificationCreate.createCommentReply(
                                reviewOwnerId, bookTitle, commenterName, reviewId
                        );

                        Map<String, String> additionalData = new HashMap<>();
                        additionalData.put("commenter_name", commenterName);
                        additionalData.put("book_title", bookTitle);

                        addNotification(notification, additionalData);
                    }
                }
            }
        }
    }

}