package com.program.bookie.models;

import java.io.Serializable;

public class BookProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    private int progressId;
    private int userId;
    private int bookId;
    private int currentPage;
    private int totalPages;
    private double progressPercentage;
    private String username;

    public BookProgress() {}

    public BookProgress(String username, int bookId, int currentPage, int totalPages) {
        this.username = username;
        this.bookId = bookId;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        updateProgressPercentage();
    }

    public BookProgress(int userId, int bookId, int currentPage, int totalPages) {
        this.userId = userId;
        this.bookId = bookId;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        updateProgressPercentage();
    }

    // Getters and Setters
    public int getProgressId() { return progressId; }
    public void setProgressId(int progressId) { this.progressId = progressId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        updateProgressPercentage();
    }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        updateProgressPercentage();
    }

    public double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    private void updateProgressPercentage() {
        this.progressPercentage = totalPages > 0 ? (double) currentPage / totalPages * 100 : 0;
        if (this.progressPercentage > 100) this.progressPercentage = 100;
        if (this.progressPercentage < 0) this.progressPercentage = 0;
    }

    @Override
    public String toString() {
        return "BookProgress{" +
                "progressId=" + progressId +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", progressPercentage=" + progressPercentage +
                ", username='" + username + '\'' +
                '}';
    }
}