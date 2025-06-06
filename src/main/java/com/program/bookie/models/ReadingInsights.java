package com.program.bookie.models;

import java.io.Serializable;

public class ReadingInsights implements Serializable {
    private static final long serialVersionUID = 1L;

    private Book userHighestRatedBook;
    private Integer userHighestRating;
    private Book mostPopularBook;
    private Book communityFavoriteBook;

    public ReadingInsights() {}

    public ReadingInsights(Book userHighestRatedBook, Integer userHighestRating,
                           Book mostPopularBook, Book communityFavoriteBook) {
        this.userHighestRatedBook = userHighestRatedBook;
        this.userHighestRating = userHighestRating;
        this.mostPopularBook = mostPopularBook;
        this.communityFavoriteBook = communityFavoriteBook;
    }

    // Getters and Setters
    public Book getUserHighestRatedBook() { return userHighestRatedBook; }
    public void setUserHighestRatedBook(Book userHighestRatedBook) { this.userHighestRatedBook = userHighestRatedBook; }

    public Integer getUserHighestRating() { return userHighestRating; }
    public void setUserHighestRating(Integer userHighestRating) { this.userHighestRating = userHighestRating; }

    public Book getMostPopularBook() { return mostPopularBook; }
    public void setMostPopularBook(Book mostPopularBook) { this.mostPopularBook = mostPopularBook; }

    public Book getCommunityFavoriteBook() { return communityFavoriteBook; }
    public void setCommunityFavoriteBook(Book communityFavoriteBook) { this.communityFavoriteBook = communityFavoriteBook; }

    @Override
    public String toString() {
        return "ReadingInsights{" +
                "userHighestRatedBook=" + (userHighestRatedBook != null ? userHighestRatedBook.getTitle() : "null") +
                ", userHighestRating=" + userHighestRating +
                ", mostPopularBook=" + (mostPopularBook != null ? mostPopularBook.getTitle() : "null") +
                ", communityFavoriteBook=" + (communityFavoriteBook != null ? communityFavoriteBook.getTitle() : "null") +
                '}';
    }
}