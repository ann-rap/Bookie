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
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @FXML private VBox reviewContentContainer;
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
        loadComments();
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
        if (currentReview.isSpoiler()) {
            handleSpoilerContent();
        } else {
            reviewTextLabel.setText(currentReview.getReviewText());
            spoilerWarning.setVisible(false);
        }
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
        // Assuming we have created_at and updated_at fields in Review model
        // For now, using placeholder dates
        dateLabel.setText("Jan 15, 2024");

        // Show "edited" label if review was modified
        // editedLabel.setVisible(review.wasEdited());
    }

    private void handleSpoilerContent() {
        if (!spoilerRevealed) {
            spoilerWarning.setVisible(true);
            reviewTextLabel.setVisible(false);
        } else {
            spoilerWarning.setVisible(false);
            reviewTextLabel.setText(currentReview.getReviewText());
            reviewTextLabel.setVisible(true);
        }
    }

    @FXML
    private void onRevealSpoilerClicked(ActionEvent event) {
        spoilerRevealed = true;
        handleSpoilerContent();
    }

    @FXML
    private void onToggleCommentsClicked(ActionEvent event) {
        commentsVisible = !commentsVisible;
        commentsContainer.setVisible(commentsVisible);

        if (commentsVisible) {
            commentsToggleButton.setText("💬 Hide comments");
            loadComments();
        } else {
            commentsToggleButton.setText("💬 Show comments (0)"); // Update with actual count
        }
    }

    @FXML
    private void onAddCommentClicked(ActionEvent event) {
        String commentText = commentTextField.getText().trim();
        if (commentText.isEmpty()) return;

        // TODO: Implement comment creation logic
        // For now, just add to UI
        addCommentToUI("You", commentText, LocalDateTime.now());
        commentTextField.clear();
    }

    private void loadComments() {
        if (!commentsVisible) return;

        // TODO: Load comments from server
        // For now, clear existing comments (except add comment section)
        commentsContainer.getChildren().clear();
        commentsContainer.getChildren().add(addCommentSection);

        // Example comment (remove when implementing real loading)
        addCommentToUI("John Doe", "Great review! I totally agree.", LocalDateTime.now().minusDays(1));
    }

    /**
     * Dodawanie komentarzy
     */
    private void addCommentToUI(String username, String text, LocalDateTime date) {
        HBox commentBox = new HBox();
        commentBox.setSpacing(8);
        commentBox.setPadding(new Insets(8, 0, 8, 0));
        commentBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8;");

        // Username label
        Label usernameLabel = new Label(username);
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        usernameLabel.setFont(Font.font(13));

        // Comment text
        Label commentLabel = new Label(text);
        commentLabel.setWrapText(true);
        commentLabel.setStyle("-fx-text-fill: #555;");
        commentLabel.setFont(Font.font(13));

        Label dateLabel = new Label(formatCommentDate(date));
        dateLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        VBox contentBox = new VBox(3);

        //username and date
        HBox headerBox = new HBox(8);
        headerBox.getChildren().addAll(usernameLabel, dateLabel);

        contentBox.getChildren().addAll(headerBox, commentLabel);

        commentBox.getChildren().add(contentBox);

        int insertIndex = commentsContainer.getChildren().size() - 1;
        commentsContainer.getChildren().add(insertIndex, commentBox);

        updateCommentsCount();
    }

    private void updateCommentsCount() {
        int commentCount = commentsContainer.getChildren().size() - 1;
        if (commentsVisible) {
            commentsToggleButton.setText("💬 Hide comments");
        } else {
            commentsToggleButton.setText("💬 Show comments (" + commentCount + ")");
        }
    }

    private String formatCommentDate(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return date.format(formatter);
    }
}