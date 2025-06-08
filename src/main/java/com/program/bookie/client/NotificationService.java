package com.program.bookie.client;

import com.program.bookie.models.*;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {
    private static NotificationService instance;
    private final Client client;

    //do GUI
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);
    private final ObservableList<INotification> notifications = FXCollections.observableArrayList();
    private AudioClip notificationSound;
    private int lastKnownCount = 0;

    private ScheduledExecutorService scheduler;
    private String currentUsername;
    private boolean isRunning = false;

    private NotificationService() {
        this.client = Client.getInstance();
        try {
            URL soundUrl = getClass().getResource("/sounds/notif.mp3");
            if (soundUrl != null) {
                notificationSound = new AudioClip(soundUrl.toString());
                notificationSound.setVolume(0.7);
                System.out.println("Notification sound loaded");
                notificationSound.play();
            } else {
                System.err.println("Sound file not found: /sounds/notif.mp3");
            }
        } catch (Exception e) {
            System.err.println("Error loading sound: " + e.getMessage());
        }
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }


    public void start(String username) {
        if (isRunning) {
            stop();
        }

        this.currentUsername = username;
        this.isRunning = true;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NotificationPoller");
            t.setDaemon(true);
            return t;
        });

        checkNotifications();

        scheduler.scheduleAtFixedRate(this::checkNotifications, 30, 30, TimeUnit.SECONDS);

        System.out.println("Notification service started for user: " + username);
    }

    public void stop() {
        isRunning = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        Platform.runLater(() -> {
            notifications.clear();
            unreadCount.set(0);
        });

        System.out.println("Notification service stopped");
    }


    private void checkNotifications() {
        if (!isRunning || currentUsername == null) return;

        try {
            Request countRequest = new Request(RequestType.GET_NOTIFICATION_COUNT, currentUsername);
            Response countResponse = client.sendRequest(countRequest);

            if (countResponse.getType() == ResponseType.SUCCESS) {
                int count = (Integer) countResponse.getData();

                Platform.runLater(() -> {
                    lastKnownCount = unreadCount.get();
                    unreadCount.set(count);

                    if (count > lastKnownCount  && notificationSound != null) {
                        System.out.println("New notifications detected! Playing sound...");
                        notificationSound.play();
                    }

                    lastKnownCount = count;
                });
            }
        } catch (Exception e) {
            System.err.println("Error checking notifications: " + e.getMessage());
        }
    }


    public void loadNotifications(boolean unreadOnly) {
        if (currentUsername == null) return;

        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("username", currentUsername);
                data.put("unreadOnly", unreadOnly);

                Request request = new Request(RequestType.GET_NOTIFICATIONS, data);
                Response response = client.sendRequest(request);

                if (response.getType() == ResponseType.SUCCESS) {
                    @SuppressWarnings("unchecked")
                    List<INotification> notificationList = (List<INotification>) response.getData();

                    Platform.runLater(() -> {
                        notifications.clear();
                        notifications.addAll(notificationList);
                    });
                }

            } catch (Exception e) {
                System.err.println("Error loading notifications: " + e.getMessage());
            }
        }).start();
    }


    public void markAsRead(List<Integer> notificationIds) {
        if (currentUsername == null || notificationIds.isEmpty()) return;

        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("username", currentUsername);
                data.put("notificationIds", notificationIds);

                Request request = new Request(RequestType.MARK_NOTIFICATIONS_READ, data);
                Response response = client.sendRequest(request);

                if (response.getType() == ResponseType.SUCCESS) {
                    Platform.runLater(() -> {
                        for (INotification notif : notifications) {
                            if (notificationIds.contains(notif.getNotificationId())) {
                                notif.setRead(true);
                            }
                        }
                        checkNotifications();
                    });
                }

            } catch (Exception e) {
                System.err.println("Error marking notifications as read: " + e.getMessage());
            }
        }).start();
    }

    public void clearAllNotifications() {
        if (currentUsername == null) return;

        new Thread(() -> {
            try {
                Request request = new Request(RequestType.CLEAR_NOTIFICATIONS, currentUsername);
                Response response = client.sendRequest(request);

                if (response.getType() == ResponseType.SUCCESS) {
                    Platform.runLater(() -> {
                        notifications.clear();
                        unreadCount.set(0);
                    });
                }

            } catch (Exception e) {
                System.err.println("Error clearing notifications: " + e.getMessage());
            }
        }).start();
    }

    public IntegerProperty unreadCountProperty() { return unreadCount; }
    public ObservableList<INotification> getNotifications() { return notifications; }
    public int getUnreadCount() { return unreadCount.get(); }
}