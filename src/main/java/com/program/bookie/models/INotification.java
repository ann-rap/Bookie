package com.program.bookie.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public interface INotification extends Serializable {
    int getNotificationId();
    void setNotificationId(int notificationId);

    int getUserId();
    void setUserId(int userId);

    String getTitle();
    String getMessage();
    String getIcon();
    String getNotificationType();

    Integer getRelatedId();
    void setRelatedId(Integer relatedId);

    boolean isRead();
    void setRead(boolean read);

    LocalDateTime getCreatedAt();
    void setCreatedAt(LocalDateTime createdAt);

    String getFormattedTime();

    void handleClick(Object controller);
}