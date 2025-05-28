package com.program.bookie.db;

import com.program.bookie.models.User;
import com.program.bookie.models.*;
import com.program.bookie.models.ResponseType.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.program.bookie.models.ResponseType.*;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/login";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "hasloProgram";

    private Connection connection;

    public DatabaseConnection() throws SQLException {
        this.connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
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

    /*
    status uzytkownika w czytaniu ksiazki
     */
    public String getStatus(String username, int bookId) throws SQLException {
        String sql = """
        SELECT b.reading_status
        FROM books b
        JOIN users u ON b.user_id = u.id
        WHERE u.username = ? AND b.book_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("reading_status");
                } else {
                    return null;
                }
            }
        }
    }
    /*Aktualizacja*/
    public void updateStatus(String username, int bookId, String newStatus) throws SQLException {

        String sqlCheck = """
        SELECT 1 FROM books b
        JOIN users u ON b.user_id = u.id
        WHERE u.username = ? AND b.book_id = ?
    """;

        String sqlUpdate = """
        UPDATE books SET reading_status = ?
        WHERE user_id = (SELECT id FROM users WHERE username = ?) AND book_id = ?
    """;

        String sqlInsert = """
        INSERT INTO books (user_id, book_id, reading_status)
        VALUES ((SELECT id FROM users WHERE username = ?), ?, ?)
    """;

        try (PreparedStatement checkStmt = connection.prepareStatement(sqlCheck)) {
            checkStmt.setString(1, username);
            checkStmt.setInt(2, bookId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Rekord istnieje - update
                    try (PreparedStatement updateStmt = connection.prepareStatement(sqlUpdate)) {
                        updateStmt.setString(1, newStatus);
                        updateStmt.setString(2, username);
                        updateStmt.setInt(3, bookId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Rekord nie istnieje - insert
                    try (PreparedStatement insertStmt = connection.prepareStatement(sqlInsert)) {
                        insertStmt.setString(1, username);
                        insertStmt.setInt(2, bookId);
                        insertStmt.setString(3, newStatus);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
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