package com.program.bookie.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class UserStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private int booksRead;
    private int booksCurrentlyReading;
    private int booksWantToRead;
    private int totalPagesRead;
    private int reviewsWritten;
    private double averageRating;
    private LocalDate accountCreatedDate;
    private long daysAsUser;

    public UserStatistics(int booksRead, int booksCurrentlyReading, int booksWantToRead,
                          int totalPagesRead, int reviewsWritten, double averageRating,
                          LocalDate accountCreatedDate) {
        this.booksRead = booksRead;
        this.booksCurrentlyReading = booksCurrentlyReading;
        this.booksWantToRead = booksWantToRead;
        this.totalPagesRead = totalPagesRead;
        this.reviewsWritten = reviewsWritten;
        this.averageRating = averageRating;
        this.accountCreatedDate = accountCreatedDate;

        // Oblicz ile dni użytkownik jest z nami
        if (accountCreatedDate != null) {
            this.daysAsUser = ChronoUnit.DAYS.between(accountCreatedDate, LocalDate.now());
        }
    }

    // Getters
    public int getBooksRead() { return booksRead; }
    public int getBooksCurrentlyReading() { return booksCurrentlyReading; }
    public int getBooksWantToRead() { return booksWantToRead; }
    public int getTotalPagesRead() { return totalPagesRead; }
    public int getReviewsWritten() { return reviewsWritten; }
    public double getAverageRating() { return averageRating; }
    public LocalDate getAccountCreatedDate() { return accountCreatedDate; }
    public long getDaysAsUser() { return daysAsUser; }

    // Setters
    public void setBooksRead(int booksRead) { this.booksRead = booksRead; }
    public void setBooksCurrentlyReading(int booksCurrentlyReading) { this.booksCurrentlyReading = booksCurrentlyReading; }
    public void setBooksWantToRead(int booksWantToRead) { this.booksWantToRead = booksWantToRead; }
    public void setTotalPagesRead(int totalPagesRead) { this.totalPagesRead = totalPagesRead; }
    public void setReviewsWritten(int reviewsWritten) { this.reviewsWritten = reviewsWritten; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setAccountCreatedDate(LocalDate accountCreatedDate) {
        this.accountCreatedDate = accountCreatedDate;
        if (accountCreatedDate != null) {
            this.daysAsUser = ChronoUnit.DAYS.between(accountCreatedDate, LocalDate.now());
        }
    }

    public int getTotalBooks() {
        return booksRead + booksCurrentlyReading + booksWantToRead;
    }

    @Override
    public String toString() {
        return "UserStatistics{" +
                "booksRead=" + booksRead +
                ", booksCurrentlyReading=" + booksCurrentlyReading +
                ", booksWantToRead=" + booksWantToRead +
                ", totalPagesRead=" + totalPagesRead +
                ", reviewsWritten=" + reviewsWritten +
                ", averageRating=" + averageRating +
                ", daysAsUser=" + daysAsUser +
                '}';
    }
}