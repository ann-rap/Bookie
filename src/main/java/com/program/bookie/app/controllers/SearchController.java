package com.program.bookie.app.controllers;

import com.program.bookie.models.*;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.program.bookie.models.ResponseType.*;
import com.program.bookie.client.Client;
import com.program.bookie.models.ResponseType.*;


import java.util.HashMap;
import java.util.Map;


public class SearchController {

    private Client client = Client.getInstance();

    @FXML
    private ImageView bookCover;
    @FXML
    private Label titleLabel;
    @FXML
    private Label authorLabel;
    @FXML
    private Label avgRatingLabel;
    @FXML
    private Label ratingCountLabel;
    @FXML
    private Label publicationYearLabel;

    @FXML
    private ComboBox<String> statusComboBox;

    private boolean isUpdating = false;



    private final String[] statuses = {"Want to read", "Currently reading", "Read"};
    private final String WANT_TO_READ = "Want to read";

    private String currentUsername;
    private int currentBookId;

    public void setData(Book book, String username) {
        this.currentUsername = username;
        this.currentBookId = book.getBookId();

        titleLabel.setText(book.getTitle());
        authorLabel.setText("by " + book.getAuthor());
        avgRatingLabel.setText(String.format("★%.1f", book.getAverageRating()) + " avg rating");
        ratingCountLabel.setText(String.valueOf(book.getRatingCount()) + " ratings");
        publicationYearLabel.setText("published in " + String.valueOf(book.getPublicationYear()));

        // Set up book cover image
        setupBookCover(book);

        // Initialize ComboBox
        initializeComboBox();

        // Load current reading status
        loadReadingStatus();
    }

    private void setupBookCover(Book book) {
        if (book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
            try {
                Image image = new Image(getClass().getResourceAsStream("/img/" + book.getCoverImagePath()));
                bookCover.setImage(image);

                double fitWidth = 150;
                double fitHeight = 200;
                bookCover.setFitWidth(fitWidth);
                bookCover.setFitHeight(fitHeight);
                bookCover.setPreserveRatio(true);
                bookCover.setSmooth(true);

                // Adjust image viewport
                double imageWidth = image.getWidth();
                double imageHeight = image.getHeight();

                double scaleX = imageWidth / fitWidth;
                double scaleY = imageHeight / fitHeight;
                double scale = Math.min(scaleX, scaleY);

                double viewportWidth = fitWidth * scale;
                double viewportHeight = fitHeight * scale;
                double viewportX = (imageWidth - viewportWidth) / 2;
                double viewportY = (imageHeight - viewportHeight) / 2;

                bookCover.setViewport(new Rectangle2D(viewportX, viewportY, viewportWidth, viewportHeight));
            } catch (Exception e) {
                System.err.println("Error loading book cover: " + e.getMessage());
            }
        }
    }

    private void initializeComboBox() {
        statusComboBox.getItems().clear();
        statusComboBox.getItems().addAll(statuses);

        // Set up the action handler
        statusComboBox.setOnAction(e -> {
            if (!isUpdating) {
                String selectedStatus = statusComboBox.getSelectionModel().getSelectedItem();
                if (selectedStatus != null) {
                    updateReadingStatus(selectedStatus);
                }
            }
        });
    }

    private void loadReadingStatus() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUsername);
            data.put("bookId", currentBookId);

            Request request = new Request(RequestType.GET_READING_STATUS, data);
            Response response = client.sendRequest(request);

            isUpdating = true; // Prevent triggering the action handler

            if (response.getType() == ResponseType.SUCCESS) {
                String status = (String) response.getData();
                if (status == null || status.trim().isEmpty()) {
                    // No status found, default to "Want to read"
                    statusComboBox.getSelectionModel().select(WANT_TO_READ);
                    setComboBoxStyle(true); // Green style for new status
                } else {
                    statusComboBox.getSelectionModel().select(status);
                    setComboBoxStyle(false); // Normal style for existing status
                }
            } else {
                System.err.println("Error loading reading status: " + response.getData());
                statusComboBox.getSelectionModel().select(WANT_TO_READ);
                setComboBoxStyle(true);
            }

        } catch (Exception e) {
            System.err.println("Exception loading reading status: " + e.getMessage());
            e.printStackTrace();
            statusComboBox.getSelectionModel().select(WANT_TO_READ);
            setComboBoxStyle(true);
        } finally {
            isUpdating = false;
        }
    }

    private void updateReadingStatus(String selectedStatus) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUsername);
            data.put("bookId", currentBookId);
            data.put("status", selectedStatus);

            Request request = new Request(RequestType.UPDATE_READING_STATUS, data);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                System.out.println("Reading status updated successfully to: " + selectedStatus);
                setComboBoxStyle(false); // Change to normal style after successful update
            } else {
                System.err.println("Error updating reading status: " + response.getData());
                // Optionally revert to previous selection or show error message
            }

        } catch (Exception e) {
            System.err.println("Exception updating reading status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setComboBoxStyle(boolean isNewStatus) {
        // Set cell factory for dropdown items
        statusComboBox.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (isNewStatus && item.equals(WANT_TO_READ)) {
                        setStyle("-fx-background-color: #658C4C; -fx-text-fill: white;");
                    } else {
                        setStyle("-fx-background-color: #d3d3d3; -fx-text-fill: black;");
                    }
                }
            }
        });

        // Set button cell for the displayed value
        statusComboBox.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (isNewStatus && item.equals(WANT_TO_READ)) {
                        setStyle("-fx-background-color: #658C4C; -fx-text-fill: white;");
                    } else {
                        setStyle("-fx-background-color: #d3d3d3; -fx-text-fill: black;");
                    }
                }
            }
        });
    }
}
