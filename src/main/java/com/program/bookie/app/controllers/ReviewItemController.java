package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class ReviewItemController implements Initializable {

    @FXML private VBox reviewContainer;
    @FXML private Label usernameLabel;
    @FXML private Label dateLabel;
    @FXML private Label editedLabel;
    @FXML private Label reviewTextLabel;
    @FXML private VBox spoilerWarning;
    @FXML private Button revealSpoilerButton;
    @FXML private StackPane reviewContentContainer;
    @FXML private VBox commentsSection;
    @FXML private Button commentsToggleButton;
    @FXML private VBox commentsContainer;
    @FXML private HBox addCommentSection;
    @FXML private TextField commentTextField;
    @FXML private Button addCommentButton;

    // Star rating elements
    @FXML private ImageView star1, star2, star3, star4, star5;

    private Client client = Client.getInstance();
    private Review currentReview;
    private String currentUsername;
    private boolean commentsVisible = false;
    private boolean spoilerRevealed = false;

    // Cache dla komentarzy
    private List<Comment> cachedComments = new ArrayList<>();
    private boolean commentsLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize component
    }

    /**
     * Sets the review data and displays it
     */
    public void setReviewData(Review review, String currentUsername) {
        this.currentReview = review;
        this.currentUsername = currentUsername;

        displayReviewData();

        // Załaduj komentarze od razu i je zachowaj
        loadAndCacheComments();
    }

    /**
     * Ładuje komentarze od razu i je cachuje
     */
    private void loadAndCacheComments() {
        if (currentReview == null) return;

        System.out.println("📝 Loading and caching comments for review ID: " + currentReview.getReviewId());

        try {
            Request request = new Request(RequestType.GET_REVIEW_COMMENTS, currentReview.getReviewId());
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                @SuppressWarnings("unchecked")
                List<Comment> comments = (List<Comment>) response.getData();

                // Zachowaj komentarze w cache
                cachedComments = comments;
                commentsLoaded = true;

                System.out.println("✅ Cached " + comments.size() + " comments");

                // Zaktualizuj przycisk z prawdziwą liczbą
                updateCommentsButtonText(comments.size());

            } else {
                System.err.println("❌ Error loading comments: " + response.getData());
                cachedComments = new ArrayList<>();
                commentsLoaded = true;
                updateCommentsButtonText(0);
            }

        } catch (Exception e) {
            System.err.println("💥 Exception loading comments: " + e.getMessage());
            cachedComments = new ArrayList<>();
            commentsLoaded = true;
            updateCommentsButtonText(0);
        }
    }

    private void displayReviewData() {
        if (currentReview == null) return;

        // Set username
        usernameLabel.setText(currentReview.getUsername());

        // Set star rating
        displayStarRating(currentReview.getRating());

        // Set dates
        displayDates();

        // Handle spoiler content
        handleSpoilerContent();
    }

    private void displayStarRating(int rating) {
        ImageView[] stars = {star1, star2, star3, star4, star5};

        for (int i = 0; i < stars.length; i++) {
            if (stars[i] != null) {
                boolean filled = i < rating;
                setStarImage(stars[i], filled);
            }
        }
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

    private void displayDates() {
        if (currentReview.getCreatedAt() != null) {
            dateLabel.setText(currentReview.getFormattedCreatedDate());
        } else {
            dateLabel.setText("Unknown date");
        }

        // Show "edited" label if review was modified
        if (editedLabel != null) {
            editedLabel.setVisible(currentReview.wasEdited());
        }
    }

    private void handleSpoilerContent() {
        if (currentReview == null) return;

        if (currentReview.isSpoiler() && !spoilerRevealed) {
            // Pokaż spoiler warning, ukryj tekst
            spoilerWarning.setVisible(true);
            spoilerWarning.setManaged(true);
            reviewTextLabel.setVisible(false);
            reviewTextLabel.setManaged(false);
        } else {
            // Ukryj spoiler warning, pokaż tekst
            spoilerWarning.setVisible(false);
            spoilerWarning.setManaged(false);

            if (currentReview.getReviewText() != null && !currentReview.getReviewText().trim().isEmpty()) {
                reviewTextLabel.setText(currentReview.getReviewText());
                reviewTextLabel.setStyle("-fx-wrap-text: true; -fx-line-spacing: 1px; -fx-text-fill: #333;");
            } else {
                reviewTextLabel.setText("No written review.");
                reviewTextLabel.setStyle("-fx-wrap-text: true; -fx-line-spacing: 1px; -fx-text-fill: #666; -fx-font-style: italic;");
            }
            reviewTextLabel.setVisible(true);
            reviewTextLabel.setManaged(true);
        }
    }

    @FXML
    private void onRevealSpoilerClicked(ActionEvent event) {
        spoilerRevealed = true;
        handleSpoilerContent();
    }

    /**
     * Obsługa kliknięcia toggle comments - tylko wyświetlanie
     */
    @FXML
    private void onToggleCommentsClicked(ActionEvent event) {
        commentsVisible = !commentsVisible;

        if (commentsVisible) {
            // Pokaż kontener i wyświetl cached komentarze
            commentsContainer.setVisible(true);
            commentsContainer.setManaged(true);

            if (commentsLoaded) {
                displayCachedComments();
            } else {
                // Fallback - jeśli jeszcze nie załadowane
                loadAndCacheComments();
            }

            updateCommentsButtonText(cachedComments.size());

        } else {
            // Ukryj kontener
            commentsContainer.setVisible(false);
            commentsContainer.setManaged(false);
            updateCommentsButtonText(cachedComments.size());
        }
    }

    /**
     * Wyświetla komentarze z cache (szybko!)
     */
    private void displayCachedComments() {
        System.out.println("🎨 Displaying " + cachedComments.size() + " cached comments");

        // Wyczyść kontener (zostaw tylko add comment section)
        commentsContainer.getChildren().clear();
        commentsContainer.getChildren().add(addCommentSection);

        // Dodaj każdy komentarz z cache
        for (Comment comment : cachedComments) {
            addCommentToUI(comment.getUsername(), comment.getContent(), comment.getCreatedAt());
        }
    }

    /**
     * Dodawanie komentarzy do UI - kompaktowa wersja
     */
    private void addCommentToUI(String username, String text, LocalDateTime date) {
        HBox commentBox = new HBox();
        commentBox.setSpacing(6);
        commentBox.setPadding(new Insets(6, 0, 6, 0));
        commentBox.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-padding: 6;");

        // Username label - mniejszy
        Label usernameLabel = new Label(username);
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 12px;");

        // Comment text - mniejszy
        Label commentLabel = new Label(text);
        commentLabel.setWrapText(true);
        commentLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");

        // Date label - mniejszy
        Label dateLabel = new Label(formatCommentDate(date));
        dateLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");

        VBox contentBox = new VBox(2); // Mniejszy spacing

        // Username and date - kompaktowo
        HBox headerBox = new HBox(6);
        headerBox.getChildren().addAll(usernameLabel, dateLabel);

        contentBox.getChildren().addAll(headerBox, commentLabel);
        commentBox.getChildren().add(contentBox);

        // Dodaj przed add comment section
        int insertIndex = commentsContainer.getChildren().size() - 1;
        commentsContainer.getChildren().add(insertIndex, commentBox);
    }

    /**
     * Dodawanie komentarza - aktualizuje cache
     */
    @FXML
    private void onAddCommentClicked(ActionEvent event) {
        String commentText = commentTextField.getText().trim();
        if (commentText.isEmpty()) {
            System.out.println("⚠️ Comment text is empty");
            return;
        }

        if (currentReview == null) {
            System.err.println("❌ currentReview is null");
            return;
        }

        System.out.println("📝 Adding comment to review ID: " + currentReview.getReviewId());

        try {
            Comment comment = new Comment();
            comment.setReviewId(currentReview.getReviewId());
            comment.setUsername(currentUsername);
            comment.setContent(commentText);

            Request request = new Request(RequestType.ADD_COMMENT, comment);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                System.out.println("✅ Comment added successfully");

                commentTextField.clear();

                // Dodaj do cache lokalnie (żeby nie robić kolejnego zapytania)
                Comment newComment = new Comment();
                newComment.setUsername(currentUsername);
                newComment.setContent(commentText);
                newComment.setCreatedAt(LocalDateTime.now());
                cachedComments.add(newComment);

                // Odśwież wyświetlanie jeśli komentarze są widoczne
                if (commentsVisible) {
                    displayCachedComments();
                }

                // Zaktualizuj licznik
                updateCommentsButtonText(cachedComments.size());

            } else {
                System.err.println("❌ Error adding comment: " + response.getData());
                showAddCommentError("Error adding comment: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("💥 Exception adding comment: " + e.getMessage());
            e.printStackTrace();
            showAddCommentError("Failed to add comment");
        }
    }

    /**
     * Aktualizuje tekst przycisku komentarzy
     */
    private void updateCommentsButtonText(int commentCount) {
        if (commentsToggleButton != null) {
            if (commentsVisible) {
                commentsToggleButton.setText("💬 Hide comments");
            } else {
                commentsToggleButton.setText("💬 Show comments (" + commentCount + ")");
            }
        }
    }

    /**
     * Pokazuje błąd dodawania komentarza
     */
    private void showAddCommentError(String message) {
        // Tymczasowo zmień kolor pola tekstowego na czerwony
        commentTextField.setStyle("-fx-background-radius: 13; -fx-padding: 4 10; -fx-font-size: 12; " +
                "-fx-border-color: red; -fx-border-width: 1;");
        commentTextField.setPromptText(message);

        // Przywróć normalny styl po 3 sekundach
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
                    commentTextField.setStyle("-fx-background-radius: 13; -fx-padding: 4 10; -fx-font-size: 12;");
                    commentTextField.setPromptText("Add a comment...");
                })
        );
        timeline.play();
    }

    /**
     * Lepsze formatowanie dat komentarzy
     */
    private String formatCommentDate(LocalDateTime date) {
        if (date == null) return "Unknown date";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return date.format(formatter);
    }
}