package com.program.bookie.models;

import java.io.Serializable;

public class Book implements Serializable {
    private static final long serialVersionUID = 1L;
    private int bookId;
    private String title;
    private String author;
    private String coverImagePath;
    private double averageRating;
    private int ratingCount;
    private int reviewCount;
    private String description;
    private String genre;
    private int publicationYear;
    private int pages;
    private int currentPage=0;

    public Book() {}

    public Book(int bookId, String title, String author, String coverImagePath,
                double averageRating, int ratingCount) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImagePath = coverImagePath;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public Book(int bookId, String title, String author, String coverImagePath,
                double averageRating, int ratingCount, String description,
                String genre, int publicationYear) {
        this(bookId, title, author, coverImagePath, averageRating, ratingCount);
        this.description = description;
        this.genre = genre;
        this.publicationYear = publicationYear;
    }

    public Book(int bookId, String title, String author, String coverImagePath,
                double averageRating, int ratingCount, String description,
                String genre, int publicationYear, int pages) {
        this(bookId, title, author, coverImagePath, averageRating, ratingCount, description, genre, publicationYear);
        this.pages = pages;
    }

    // Getters
    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCoverImagePath() { return coverImagePath; }
    public double getAverageRating() { return averageRating; }
    public int getRatingCount() { return ratingCount; }
    public String getDescription() { return description; }
    public String getGenre() { return genre; }
    public int getPublicationYear() { return publicationYear; }

    public int getPages() { return pages; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    // Setters
    public void setBookId(int bookId) { this.bookId = bookId; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public void setDescription(String description) { this.description = description; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }
    public void setPages(int pages) { this.pages = pages; }

    @Override
    public String toString() {
        return "Book{" +
                "bookId=" + bookId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", averageRating=" + averageRating +
                ", ratingCount=" + ratingCount +
                ", pages=" + pages +
                '}';
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Oblicza postęp w procentach
     */
    public double getProgressPercentage() {
        int total = getPages();
        if (total <= 0) return 0.0;

        double percentage = (currentPage * 100.0) / total;
        return Math.min(100.0, Math.max(0.0, percentage));
    }

    /**
     * Sprawdza czy książka jest ukończona
     */
    public boolean isCompleted() {
        return getCurrentPage() >= getPages() && getPages() > 0;
    }
}