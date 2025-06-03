package com.program.bookie.models;

import java.io.Serializable;

public class Book implements Serializable {
    private int bookId;
    private String title;
    private String author;
    private String coverImagePath;
    private double averageRating;
    private int ratingCount;
    private String description;
    private String genre;
    private int publicationYear;

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


    @Override
    public String toString() {
        return "Book{" +
                "bookId=" + bookId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", averageRating=" + averageRating +
                ", ratingCount=" + ratingCount +
                '}';
    }
}