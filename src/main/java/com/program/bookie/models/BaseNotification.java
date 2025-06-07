package com.program.bookie.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseNotification implements INotification {
    private static final long serialVersionUID = 1L;

    protected int notificationId;
    protected int userId;
    protected String title;
    protected String message;
    protected Integer relatedId;
    protected boolean isRead;
    protected LocalDateTime createdAt;

    public BaseNotification() {
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public BaseNotification(int userId, String title, String message, Integer relatedId) {
        this();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.relatedId = relatedId;
    }

    @Override
    public int getNotificationId() { return notificationId; }

    @Override
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    @Override
    public int getUserId() { return userId; }

    @Override
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public String getTitle() { return title; }

    @Override
    public String getMessage() { return message; }

    @Override
    public Integer getRelatedId() { return relatedId; }

    @Override
    public void setRelatedId(Integer relatedId) { this.relatedId = relatedId; }

    @Override
    public boolean isRead() { return isRead; }

    @Override
    public void setRead(boolean read) { isRead = read; }

    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String getFormattedTime() {
        if (createdAt == null) return "Unknown time";

        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = java.time.temporal.ChronoUnit.MINUTES.between(createdAt, now);

        if (minutesAgo < 1) {
            return "Just now";
        } else if (minutesAgo < 60) {
            return minutesAgo + "m ago";
        } else if (minutesAgo < 1440) { // 24 hours
            return (minutesAgo / 60) + "h ago";
        } else {
            return createdAt.format(DateTimeFormatter.ofPattern("MMM dd"));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + notificationId +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}
