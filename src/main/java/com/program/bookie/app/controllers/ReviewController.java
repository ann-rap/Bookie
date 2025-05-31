package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class ReviewController implements Initializable {

    @FXML
    private ImageView star1, star2, star3, star4, star5;

    @FXML
    private Button closeButton, reviewButton;

    @FXML
    private TextArea reviewText;

    @FXML
    private Label titleLabel, authorLabel;

    @FXML
    private ImageView bookCoverImageView;

    private int currentRating = 0;
    private Client client = Client.getInstance();
    private Book currentBook;
    private String currentUsername;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        resetStars();
    }

    public void setBookData(Book book, String username, boolean editMode) {
        this.currentBook = book;
        this.currentUsername = username;
        this.isEditMode = editMode;

        // Ustaw podstawowe informacje o książce
        if (titleLabel != null) {
            titleLabel.setText(book.getTitle());
        }
        if (authorLabel != null) {
            authorLabel.setText(book.getAuthor());
        }

        // Ustaw okładkę książki
        setupBookCover(book);

        // Jeśli to tryb edycji, załaduj istniejące dane
        if (editMode) {
            loadExistingReview();
            reviewButton.setText("Update review");
        } else {
            resetStars();
            reviewText.clear();
            reviewButton.setText("Add a review");
        }
    }

    private void setupBookCover(Book book) {
        if (bookCoverImageView != null && book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
            try {
                String fullResourcePath = "/img/" + book.getCoverImagePath();
                URL imageUrl = getClass().getResource(fullResourcePath);
                if (imageUrl != null) {
                    Image image = new Image(imageUrl.toString());
                    bookCoverImageView.setImage(image);
                } else {
                    System.err.println("Image not found: " + fullResourcePath);
                }
            } catch (Exception e) {
                System.err.println("Error loading book cover: " + e.getMessage());
            }
        }
    }

    private void loadExistingReview() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUsername);
            data.put("bookId", currentBook.getBookId());

            Request request = new Request(RequestType.GET_USER_REVIEW, data);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                Review review = (Review) response.getData();
                if (review != null) {
                    // Ustaw rating i gwiazdki
                    currentRating = review.getRating();
                    updateStarsDisplay();

                    // Ustaw tekst review
                    if (review.getReviewText() != null) {
                        reviewText.setText(review.getReviewText());
                    }
                }
            } else {
                System.err.println("Error loading review: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("Exception loading review: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void closeButtonOnAction(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    public void star1OnAction(ActionEvent event) {
        currentRating = 1;
        updateStarsDisplay();
    }

    public void star2OnAction(ActionEvent event) {
        currentRating = 2;
        updateStarsDisplay();
    }

    public void star3OnAction(ActionEvent event) {
        currentRating = 3;
        updateStarsDisplay();
    }

    public void star4OnAction(ActionEvent event) {
        currentRating = 4;
        updateStarsDisplay();
    }

    public void star5OnAction(ActionEvent event) {
        currentRating = 5;
        updateStarsDisplay();
    }

    private void updateStarsDisplay() {
        ImageView[] stars = {star1, star2, star3, star4, star5};

        for (int i = 0; i < stars.length; i++) {
            if (stars[i] != null) {
                boolean filled = i < currentRating;
                setStarImage(stars[i], filled);
            }
        }
    }

    private void resetStars() {
        currentRating = 0;
        updateStarsDisplay();
    }

    private void setStarImage(ImageView star, boolean filled) {
        try {
            String imagePath = filled ? "/img/star.png" : "/img/star2.png";
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            star.setImage(image);
        } catch (Exception e) {
            System.err.println("Error loading star image: " + e.getMessage());
        }
    }

    public void onReviewButtonAction(ActionEvent event) {
        if (currentRating == 0) {
            // Można dodać alert, że trzeba wybrać rating
            System.out.println("Please select a rating before submitting");
            return;
        }

        String reviewTextValue = reviewText.getText().trim();

        try {
            Review review = new Review(currentUsername, currentBook.getBookId(), currentRating, reviewTextValue);

            Request request = new Request(RequestType.SAVE_USER_REVIEW, review);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                System.out.println("Review saved successfully!");

                // Zamknij okno po pomyślnym zapisie
                Stage stage = (Stage) reviewButton.getScene().getWindow();
                stage.close();

            } else {
                System.err.println("Error saving review: " + response.getData());
                // Można dodać alert z błędem
            }

        } catch (Exception e) {
            System.err.println("Exception saving review: " + e.getMessage());
            e.printStackTrace();
        }
    }
}