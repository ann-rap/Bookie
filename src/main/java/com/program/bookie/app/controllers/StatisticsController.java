package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.application.Platform;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Locale;
import com.program.bookie.models.ReadingInsights;

public class StatisticsController implements Initializable {

    @FXML
    private Label booksReadLabel;
    @FXML
    private Label daysAsUserLabel;
    @FXML
    private Label pagesReadLabel;
    @FXML
    private Label reviewsWrittenLabel;
    @FXML
    private Label currentlyReadingLabel;
    @FXML
    private Label wantToReadLabel;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private Label memberSinceLabel;
    @FXML
    private Label journeyLabel;
    @FXML
    private Label highestRatedBookLabel;
    @FXML
    private Label highestRatedRatingLabel;
    @FXML
    private Label mostPopularBookLabel;
    @FXML
    private Label mostPopularCountLabel;
    @FXML
    private Label communityFavoriteBookLabel;
    @FXML
    private Label communityFavoriteRatingLabel;


    private Client client = Client.getInstance();
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        resetLabels();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadUserStatistics();
    }

    private void resetLabels() {
        Platform.runLater(() -> {
            booksReadLabel.setText("0");
            daysAsUserLabel.setText("0");
            pagesReadLabel.setText("0");
            reviewsWrittenLabel.setText("0");
            currentlyReadingLabel.setText("0");
            wantToReadLabel.setText("0");
            averageRatingLabel.setText("0.0");
            memberSinceLabel.setText("2024");
            journeyLabel.setText("Loading your reading statistics...");

            if (highestRatedBookLabel != null) highestRatedBookLabel.setText("Loading...");
            if (highestRatedRatingLabel != null) highestRatedRatingLabel.setText("★ 0.0");
            if (mostPopularBookLabel != null) mostPopularBookLabel.setText("Loading...");
            if (mostPopularCountLabel != null) mostPopularCountLabel.setText("0 reviews");
            if (communityFavoriteBookLabel != null) communityFavoriteBookLabel.setText("Loading...");
            if (communityFavoriteRatingLabel != null) communityFavoriteRatingLabel.setText("★ 0.0");
        });
    }

    private void loadUserStatistics() {
        if (currentUser == null) {
            System.err.println("Current user is null");
            return;
        }

        try {
            Request request = new Request(RequestType.GET_USER_STATISTICS, currentUser.getUsername());
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                UserStatistics stats = (UserStatistics) response.getData();
                updateStatisticsDisplay(stats);
                loadReadingInsights();
            } else {
                System.err.println("Error loading user statistics: " + response.getData());
                setErrorMessage();
            }

        } catch (Exception e) {
            System.err.println("Exception loading user statistics: " + e.getMessage());
            e.printStackTrace();
            setErrorMessage();
        }
    }

    private void loadReadingInsights() {
        if (currentUser == null) {
            System.err.println("Current user is null");
            return;
        }

        try {
            Request request = new Request(RequestType.GET_READING_INSIGHTS, currentUser.getUsername());
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                ReadingInsights insights = (ReadingInsights) response.getData();
                updateReadingInsightsUI(insights);
            } else {
                System.err.println("Error loading reading insights: " + response.getData());
                setInsightsErrorMessage();
            }

        } catch (Exception e) {
            System.err.println("Exception loading reading insights: " + e.getMessage());
            e.printStackTrace();
            setInsightsErrorMessage();
        }
    }

    private void updateStatisticsDisplay(UserStatistics stats) {
        Platform.runLater(() -> {
            booksReadLabel.setText(String.valueOf(stats.getBooksRead()));
            daysAsUserLabel.setText(String.valueOf(stats.getDaysAsUser()));
            pagesReadLabel.setText(String.format("%,d", stats.getTotalPagesRead()));
            reviewsWrittenLabel.setText(String.valueOf(stats.getReviewsWritten()));
            currentlyReadingLabel.setText(String.valueOf(stats.getBooksCurrentlyReading()));
            wantToReadLabel.setText(String.valueOf(stats.getBooksWantToRead()));

            if (stats.getAverageRating() > 0) {
                averageRatingLabel.setText(String.format("★%.1f", stats.getAverageRating()));
            } else {
                averageRatingLabel.setText("N/A");
            }

            if (stats.getAccountCreatedDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
                memberSinceLabel.setText(stats.getAccountCreatedDate().format(formatter));
            }

            updateJourneyMessage(stats);
        });
    }

    private void updateReadingInsightsUI(ReadingInsights insights) {
        Platform.runLater(() -> {
            if (insights.getUserHighestRatedBook() != null) {
                Book book = insights.getUserHighestRatedBook();
                String displayText = book.getTitle() + " by " + book.getAuthor();
                highestRatedBookLabel.setText(displayText);
                if (insights.getUserHighestRating() != null) {
                    highestRatedRatingLabel.setText("★ " + insights.getUserHighestRating() + ".0");
                } else {
                    highestRatedRatingLabel.setText("★ 5.0");
                }
            } else {
                highestRatedBookLabel.setText("No rated books yet");
                highestRatedRatingLabel.setText("★ 0.0");
            }

            if (insights.getMostPopularBook() != null) {
                Book book = insights.getMostPopularBook();
                String displayText = book.getTitle() + " by " + book.getAuthor();
                mostPopularBookLabel.setText(displayText);
                mostPopularCountLabel.setText(book.getReviewCount() + " reviews");
            } else {
                mostPopularBookLabel.setText("No popular books found");
                mostPopularCountLabel.setText("0 reviews");
            }

            if (insights.getCommunityFavoriteBook() != null) {
                Book book = insights.getCommunityFavoriteBook();
                String displayText = book.getTitle() + " by " + book.getAuthor();
                communityFavoriteBookLabel.setText(displayText);
                communityFavoriteRatingLabel.setText(String.format("★ %.1f", book.getAverageRating()));
            } else {
                communityFavoriteBookLabel.setText("No community favorites");
                communityFavoriteRatingLabel.setText("★ 0.0");
            }
        });
    }

    private void updateJourneyMessage(UserStatistics stats) {
        StringBuilder journey = new StringBuilder();

        if (stats.getBooksRead() == 0) {
            journey.append("Ready to start your reading adventure? Add your first book!");
        } else if (stats.getBooksRead() == 1) {
            journey.append("Great start! You've completed your first book. Many more adventures await!");
        } else if (stats.getBooksRead() < 5) {
            journey.append("You're building a great reading habit! ")
                    .append(stats.getBooksRead())
                    .append(" books completed in ")
                    .append(stats.getDaysAsUser())
                    .append(" days.");
        } else if (stats.getBooksRead() < 10) {
            journey.append("Impressive reading journey! ")
                    .append(stats.getBooksRead())
                    .append(" books and counting. You're becoming a true bookworm!");
        } else if (stats.getBooksRead() < 25) {
            journey.append("Amazing dedication! ")
                    .append(stats.getBooksRead())
                    .append(" books completed. You're an avid reader!");
        } else {
            journey.append("Incredible! ")
                    .append(stats.getBooksRead())
                    .append(" books completed! You're a reading champion!");
        }

        // Dodaj informację o stronach
        if (stats.getTotalPagesRead() > 1000) {
            journey.append(" That's over ")
                    .append(String.format("%,d", stats.getTotalPagesRead()))
                    .append(" pages of knowledge and stories!");
        }

        journeyLabel.setText(journey.toString());
    }

    private void setDefaultReadingInsights() {
        Platform.runLater(() -> {
            highestRatedBookLabel.setText("No data available");
            highestRatedRatingLabel.setText("★ 0.0");

            mostPopularBookLabel.setText("No data available");
            mostPopularCountLabel.setText("0 reviews");

            communityFavoriteBookLabel.setText("No data available");
            communityFavoriteRatingLabel.setText("★ 0.0");
        });
    }

    private void setErrorMessage() {
        Platform.runLater(() -> {
            journeyLabel.setText("Unable to load your statistics. Please try again later.");
        });
    }

    private void setInsightsErrorMessage() {
        Platform.runLater(() -> {
            if (highestRatedBookLabel != null) highestRatedBookLabel.setText("Unable to load");
            if (mostPopularBookLabel != null) mostPopularBookLabel.setText("Unable to load");
            if (communityFavoriteBookLabel != null) communityFavoriteBookLabel.setText("Unable to load");
        });
    }
}