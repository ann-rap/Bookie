package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StatisticsController implements Initializable {

    @FXML
    private Label booksReadLabel;
    @FXML
    private Label currentlyReadingLabel;
    @FXML
    private Label daysAsUserLabel;
    @FXML
    private Label wantToReadLabel;
    @FXML
    private Label pagesReadLabel;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private Label reviewsWrittenLabel;
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

    private User currentUser;
    private Client client = Client.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("StatisticsController initialized");
        resetLabels();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            System.out.println("Setting current user: " + user.getUsername());
            loadUserStatistics();
        }
    }

    public void refreshStatistics() {
        if (currentUser != null) {
            System.out.println("Refreshing statistics for user: " + currentUser.getUsername());
            loadUserStatistics();
        }
    }

    private void resetLabels() {
        Platform.runLater(() -> {
            if (booksReadLabel != null) booksReadLabel.setText("0");
            if (currentlyReadingLabel != null) currentlyReadingLabel.setText("0");
            if (daysAsUserLabel != null) daysAsUserLabel.setText("0");
            if (wantToReadLabel != null) wantToReadLabel.setText("0");
            if (pagesReadLabel != null) pagesReadLabel.setText("0");
            if (averageRatingLabel != null) averageRatingLabel.setText("★ 0.0");
            if (reviewsWrittenLabel != null) reviewsWrittenLabel.setText("0");
            if (memberSinceLabel != null) memberSinceLabel.setText("2024");
            if (journeyLabel != null) journeyLabel.setText("Loading your reading statistics...");

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
            System.err.println("Current user is null, cannot load statistics");
            return;
        }

        System.out.println("Loading statistics for user: " + currentUser.getUsername());

        new Thread(() -> {
            try {
                Request request = new Request(RequestType.GET_USER_STATISTICS, currentUser.getUsername());
                Response response = client.sendRequest(request);

                if (response.getType() == ResponseType.SUCCESS) {
                    Object data = response.getData();
                    System.out.println("SERVER RETURNED: " + data.getClass().getName());

                    if (data instanceof UserStatistics) {
                        UserStatistics stats = (UserStatistics) data;
                        Platform.runLater(() -> {
                            updateStatisticsDisplay(stats);
                        });
                    } else {
                        System.err.println("Unexpected data type: " + data.getClass().getName());
                        Platform.runLater(() -> {
                            setDefaultStatistics();
                        });
                    }

                    loadReadingInsights();

                } else {
                    System.err.println("Error loading statistics: " + response.getData());
                    Platform.runLater(() -> {
                        setDefaultStatistics();
                        setErrorMessage();
                    });
                }

            } catch (Exception e) {
                System.err.println("Exception loading statistics: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    setDefaultStatistics();
                    setErrorMessage();
                });
            }
        }).start();
    }

    private void loadReadingInsights() {
        if (currentUser == null) {
            System.err.println("Current user is null");
            return;
        }

        new Thread(() -> {
            try {
                Request request = new Request(RequestType.GET_READING_INSIGHTS, currentUser.getUsername());
                Response response = client.sendRequest(request);

                if (response.getType() == ResponseType.SUCCESS) {
                    ReadingInsights insights = (ReadingInsights) response.getData();
                    Platform.runLater(() -> {
                        updateReadingInsightsUI(insights);
                    });
                } else {
                    System.err.println("Error loading reading insights: " + response.getData());
                    Platform.runLater(() -> {
                        setInsightsErrorMessage();
                    });
                }

            } catch (Exception e) {
                System.err.println("Exception loading reading insights: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    setInsightsErrorMessage();
                });
            }
        }).start();
    }

    private void updateStatisticsDisplay(UserStatistics stats) {
        try {
            System.out.println("Updating statistics UI with: " + stats.toString());

            if (booksReadLabel != null) {
                booksReadLabel.setText(String.valueOf(stats.getBooksRead()));
            }

            if (currentlyReadingLabel != null) {
                currentlyReadingLabel.setText(String.valueOf(stats.getBooksCurrentlyReading()));
            }

            if (daysAsUserLabel != null) {
                daysAsUserLabel.setText(String.valueOf(stats.getDaysAsUser()));
            }

            if (wantToReadLabel != null) {
                wantToReadLabel.setText(String.valueOf(stats.getBooksWantToRead()));
            }

            if (pagesReadLabel != null) {
                pagesReadLabel.setText(String.format("%,d", stats.getTotalPagesRead()));
            }

            if (reviewsWrittenLabel != null) {
                reviewsWrittenLabel.setText(String.valueOf(stats.getReviewsWritten()));
            }

            if (averageRatingLabel != null) {
                if (stats.getAverageRating() > 0) {
                    averageRatingLabel.setText(String.format("★ %.1f", stats.getAverageRating()));
                } else {
                    averageRatingLabel.setText("N/A");
                }
            }

            if (memberSinceLabel != null) {
                if (stats.getAccountCreatedDate() != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
                    memberSinceLabel.setText(stats.getAccountCreatedDate().format(formatter));
                } else {
                    memberSinceLabel.setText("2024");
                }
            }

            updateJourneyMessage(stats);

            System.out.println("Statistics UI updated successfully");

        } catch (Exception e) {
            System.err.println("Error updating statistics UI: " + e.getMessage());
            e.printStackTrace();
            setDefaultStatistics();
        }
    }

    private void updateReadingInsightsUI(ReadingInsights insights) {
        try {
            if (insights.getUserHighestRatedBook() != null) {
                Book book = insights.getUserHighestRatedBook();
                String displayText = book.getTitle() + " by " + book.getAuthor();
                if (highestRatedBookLabel != null) {
                    highestRatedBookLabel.setText(displayText);
                }
                if (highestRatedRatingLabel != null) {
                    if (insights.getUserHighestRating() != null) {
                        highestRatedRatingLabel.setText("★ " + insights.getUserHighestRating() + ".0");
                    } else {
                        highestRatedRatingLabel.setText("★ 5.0");
                    }
                }
            } else {
                if (highestRatedBookLabel != null) highestRatedBookLabel.setText("No rated books yet");
                if (highestRatedRatingLabel != null) highestRatedRatingLabel.setText("★ 0.0");
            }

            if (insights.getMostPopularBook() != null) {
                Book book = insights.getMostPopularBook();
                String displayText = book.getTitle() + " by " + book.getAuthor();
                if (mostPopularBookLabel != null) {
                    mostPopularBookLabel.setText(displayText);
                }
                if (mostPopularCountLabel != null) {
                    mostPopularCountLabel.setText(book.getRatingCount() + " reviews");
                }
            } else {
                if (mostPopularBookLabel != null) mostPopularBookLabel.setText("No popular books found");
                if (mostPopularCountLabel != null) mostPopularCountLabel.setText("0 reviews");
            }

            if (insights.getCommunityFavoriteBook() != null) {
                Book book = insights.getCommunityFavoriteBook();
                String displayText = book.getTitle() + " by " + book.getAuthor();
                if (communityFavoriteBookLabel != null) {
                    communityFavoriteBookLabel.setText(displayText);
                }
                if (communityFavoriteRatingLabel != null) {
                    communityFavoriteRatingLabel.setText(String.format("★ %.1f", book.getAverageRating()));
                }
            } else {
                if (communityFavoriteBookLabel != null) communityFavoriteBookLabel.setText("No community favorites");
                if (communityFavoriteRatingLabel != null) communityFavoriteRatingLabel.setText("★ 0.0");
            }

        } catch (Exception e) {
            System.err.println("Error updating reading insights UI: " + e.getMessage());
            e.printStackTrace();
            setInsightsErrorMessage();
        }
    }

    private void updateJourneyMessage(UserStatistics stats) {
        if (journeyLabel == null) return;

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

        if (stats.getTotalPagesRead() > 1000) {
            journey.append(" That's over ")
                    .append(String.format("%,d", stats.getTotalPagesRead()))
                    .append(" pages of knowledge and stories!");
        }

        String journeyText = journey.toString();
        journeyLabel.setText(journeyText);

        adjustJourneyLabelFontSize(journeyText);
    }

    private void adjustJourneyLabelFontSize(String text) {
        if (journeyLabel == null || text == null) return;

        int textLength = text.length();
        double fontSize;

        if (textLength <= 60) {
            fontSize = 20.0;
        } else if (textLength <= 100) {
            fontSize = 19.0;
        } else if (textLength <= 140) {
            fontSize = 17.0;
        } else if (textLength <= 180) {
            fontSize = 15.0;
        } else {
            fontSize = 13.0;
        }

        journeyLabel.setStyle("-fx-font-size: " + fontSize + "px;");

        System.out.println("Journey text length: " + textLength + ", font size set to: " + fontSize + "px");
    }

    private void setDefaultStatistics() {
        System.out.println("Setting default statistics");

        if (daysAsUserLabel != null) daysAsUserLabel.setText("0");
        if (booksReadLabel != null) booksReadLabel.setText("0");
        if (currentlyReadingLabel != null) currentlyReadingLabel.setText("0");
        if (wantToReadLabel != null) wantToReadLabel.setText("0");
        if (pagesReadLabel != null) pagesReadLabel.setText("0");
        if (averageRatingLabel != null) averageRatingLabel.setText("★ 0.0");
        if (reviewsWrittenLabel != null) reviewsWrittenLabel.setText("0");
        if (memberSinceLabel != null) memberSinceLabel.setText("2024");
        if (highestRatedBookLabel != null) highestRatedBookLabel.setText("Loading...");
        if (highestRatedRatingLabel != null) highestRatedRatingLabel.setText("★ 0.0");
        if (mostPopularBookLabel != null) mostPopularBookLabel.setText("Loading...");
        if (mostPopularCountLabel != null) mostPopularCountLabel.setText("0 reviews");
        if (communityFavoriteBookLabel != null) communityFavoriteBookLabel.setText("Loading...");
        if (communityFavoriteRatingLabel != null) communityFavoriteRatingLabel.setText("★ 0.0");
    }

    private void setErrorMessage() {
        if (journeyLabel != null) {
            journeyLabel.setText("Unable to load your statistics. Please try again later.");
        }
    }

    private void setInsightsErrorMessage() {
        if (highestRatedBookLabel != null) highestRatedBookLabel.setText("Unable to load");
        if (mostPopularBookLabel != null) mostPopularBookLabel.setText("Unable to load");
        if (communityFavoriteBookLabel != null) communityFavoriteBookLabel.setText("Unable to load");
    }
}
