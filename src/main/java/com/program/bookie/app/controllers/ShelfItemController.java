package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class ShelfItemController implements Initializable {

    @FXML private ImageView bookCover;
    @FXML private Label titleLabel;
    @FXML private Label authorLabel;
    @FXML private ProgressBar readingProgress;
    @FXML private Label progressLabel;
    @FXML private Button editProgressButton;
    @FXML private VBox mainContainer;

    private Client client = Client.getInstance();
    private Book currentBook;
    private String currentUsername;
    private String currentStatus;
    private MainController mainController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupClickableArea();
    }

    public void setData(Book book, String username, String status, MainController mainController) {
        this.currentBook = book;
        this.currentUsername = username;
        this.currentStatus = status;
        this.mainController = mainController;

        titleLabel.setText(book.getTitle());
        authorLabel.setText("by " + book.getAuthor());

        adjustFontSizes();

        setupBookCover(book);
        loadProgress();
    }

    private void setupClickableArea() {
        if (mainContainer != null) {
            mainContainer.setPrefHeight(350.0);
            mainContainer.setMaxHeight(350.0);
            mainContainer.setMinHeight(350.0);

            mainContainer.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-padding: 10;" +
                            "-fx-background-radius: 8;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);" +
                            "-fx-cursor: hand;" +
                            "-fx-border-width: 0;"
            );

            mainContainer.setOnMouseEntered(e -> {
                mainContainer.setStyle(
                        "-fx-background-color: #f8f9fa;" +
                                "-fx-padding: 10;" +
                                "-fx-background-radius: 8;" +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 3);" +
                                "-fx-cursor: hand;" +
                                "-fx-border-width: 0;"
                );
            });

            mainContainer.setOnMouseExited(e -> {
                mainContainer.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-padding: 10;" +
                                "-fx-background-radius: 8;" +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);" +
                                "-fx-cursor: hand;" +
                                "-fx-border-width: 0;"
                );
            });

            mainContainer.setOnMouseClicked(e -> {
                if (e.getTarget() == editProgressButton || isClickOnButton(e.getTarget())) {
                    return;
                }

                if (mainController != null && currentBook != null) {
                    mainController.showBookDetails(currentBook);
                }
            });
        }
    }

    private boolean isClickOnButton(Object target) {
        if (target instanceof javafx.scene.Node) {
            javafx.scene.Node node = (javafx.scene.Node) target;
            javafx.scene.Node parent = node.getParent();

            while (parent != null) {
                if (parent == editProgressButton) {
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    private void setupBookCover(Book book) {
        if (bookCover == null) return;

        String imagePath = book.getCoverImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            setDefaultCover();
            return;
        }

        // Zmniejszone wymiary okładki
        bookCover.setFitWidth(100);
        bookCover.setFitHeight(140);
        bookCover.setPreserveRatio(true);
        bookCover.setSmooth(true);

        Image cachedImage = client.getImageFX(imagePath);
        if (cachedImage != null) {
            setImageWithViewport(cachedImage);
            return;
        }

        new Thread(() -> {
            try {
                Image serverImage = client.getImageFX(imagePath);
                Platform.runLater(() -> {
                    if (serverImage != null) {
                        setImageWithViewport(serverImage);
                    } else {
                        setDefaultCover();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(this::setDefaultCover);
            }
        }).start();
    }

    private void setImageWithViewport(Image image) {
        if (bookCover == null || image == null) return;
        bookCover.setImage(image);

        double fitWidth = 100;
        double fitHeight = 140;
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        if (imageWidth > 0 && imageHeight > 0) {
            double scaleX = imageWidth / fitWidth;
            double scaleY = imageHeight / fitHeight;
            double scale = Math.min(scaleX, scaleY);

            double viewportWidth = fitWidth * scale;
            double viewportHeight = fitHeight * scale;
            double viewportX = Math.max(0, (imageWidth - viewportWidth) / 2);
            double viewportY = Math.max(0, (imageHeight - viewportHeight) / 2);

            bookCover.setViewport(new Rectangle2D(viewportX, viewportY, viewportWidth, viewportHeight));
        }
    }

    private void setDefaultCover() {
        if (bookCover != null) {
            bookCover.setImage(null);
            bookCover.setStyle("-fx-background-color: #dddddd; -fx-border-color: #cccccc; -fx-border-width: 1;");
        }
    }

    private void loadProgress() {
        showProgressElements();

        if (!"Currently reading".equals(currentStatus)) {
            if ("Read".equals(currentStatus)) {
                updateProgressDisplay(100.0);
            } else {
                updateProgressDisplay(0.0);
            }
            return;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUsername);
            data.put("bookId", currentBook.getBookId());

            Request request = new Request(RequestType.GET_BOOK_PROGRESS, data);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                BookProgress progress = (BookProgress) response.getData();
                if (progress != null) {
                    updateProgressDisplay(progress.getProgressPercentage());
                } else {
                    updateProgressDisplay(0);
                }
            } else {
                updateProgressDisplay(0);
            }

        } catch (Exception e) {
            System.err.println("Error loading progress: " + e.getMessage());
            updateProgressDisplay(0);
        }
    }

    private void showProgressElements() {
        Platform.runLater(() -> {
            if (readingProgress != null) readingProgress.setVisible(true);
            if (progressLabel != null) progressLabel.setVisible(true);
            if (editProgressButton != null) editProgressButton.setVisible(true);
        });
    }

    private void updateProgressDisplay(double percentage) {
        Platform.runLater(() -> {
            if (readingProgress != null && progressLabel != null) {
                readingProgress.setProgress(percentage / 100.0);
                progressLabel.setText(String.format("%.0f%%", percentage));

                readingProgress.setStyle(
                        "-fx-accent: #658C4C;" +
                                "-fx-control-inner-background: white;" +
                                "-fx-background-color: white;" +
                                "-fx-border-color: #cccccc;" +
                                "-fx-border-width: 1px;" +
                                "-fx-pref-height: 16px;" +
                                "-fx-max-height: 16px;" +
                                "-fx-min-height: 16px;"
                );

                readingProgress.setVisible(true);
                progressLabel.setVisible(true);

                editProgressButton.setVisible(true);
            }
        });
    }

    @FXML
    private void onEditProgressClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/progressEdit.fxml"));
            AnchorPane progressPane = loader.load();

            ProgressEditController controller = loader.getController();
            controller.setBookData(currentBook, currentUsername);

            controller.setProgressUpdateCallback(progress -> {
                Platform.runLater(() -> {
                    loadProgress();
                    if (mainController != null) {
                        mainController.refreshShelvesIfNeeded();
                    }
                });
            });

            Stage progressStage = new Stage();
            progressStage.initStyle(StageStyle.UNDECORATED);
            progressStage.setScene(new Scene(progressPane, 480, 400));
            progressStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icon.png"))));
            progressStage.setResizable(false);
            progressStage.show();

        } catch (Exception e) {
            System.err.println("Error opening progress edit window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void adjustFontSizes() {
        String title = titleLabel.getText();
        int fontSize = 16;

        if (title.length() > 30) {
            fontSize = 12;
        } else if (title.length() > 20) {
            fontSize = 14;
        }

        titleLabel.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; -fx-text-fill: #333;");

        String author = authorLabel.getText();
        int authorFontSize = 13;

        if (author.length() > 25) {
            authorFontSize = 10;
        } else if (author.length() > 20) {
            authorFontSize = 11;
        }

        authorLabel.setStyle("-fx-font-size: " + authorFontSize + "px; -fx-text-fill: #666;");
    }
}