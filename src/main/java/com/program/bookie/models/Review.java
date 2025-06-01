package com.program.bookie.models;

import java.io.Serializable;

public class Review implements Serializable {
    private static final long serialVersionUID = 1L;

    private int reviewId;
    private int userId;
    private int bookId;
    private int rating;
    private String reviewText;
    private boolean isSpoiler;
    private String username;

    public Review() {}

    public Review(int userId, int bookId, int rating, String reviewText,boolean isSpoiler) {
        this.userId = userId;
        this.bookId = bookId;
        this.rating = rating;
        this.reviewText = reviewText;
        this.isSpoiler = isSpoiler;
    }

    public Review(String username, int bookId, int rating, String reviewText,boolean isSpoiler) {
        this.username = username;
        this.bookId = bookId;
        this.rating = rating;
        this.reviewText = reviewText;
        this.isSpoiler = isSpoiler;
    }

    // Getters and Setters
    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isSpoiler() { return isSpoiler; }
    public void setSpoiler(boolean spoiler) { isSpoiler = spoiler; }


    @Override
    public String toString() {
        return "Review{" +
                "reviewId=" + reviewId +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", rating=" + rating +
                ", reviewText='" + reviewText + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}