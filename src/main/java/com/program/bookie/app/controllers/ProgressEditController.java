package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class ProgressEditController implements Initializable {

    @FXML
    private Label bookTitleLabel;
    @FXML
    private Label bookAuthorLabel;
    @FXML
    private ImageView bookCoverImage;
    @FXML
    private TextField currentPageField;
    @FXML
    private TextField totalPagesField;
    @FXML
    private Slider progressSlider;
    @FXML
    private Label progressLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label titleLabel;

    private Book currentBook;
    private String currentUsername;
    private Client client = Client.getInstance();
    private Consumer<Book> progressUpdateCallback;

    private boolean isUpdatingFromSlider = false;
    private boolean isUpdatingFromText = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            if (progressSlider != null) {
                progressSlider.setStyle(
                        "-fx-control-inner-background: white;" +
                                "-fx-background-color: white;" +
                                "-fx-border-color: #cccccc;" +
                                "-fx-border-width: 1px;" +
                                "-fx-border-radius: 10px;" +
                                "-fx-background-radius: 10px;"
                );

                progressSlider.lookup(".thumb").setStyle("-fx-background-color: #658C4C;");
                progressSlider.lookup(".track").setStyle("-fx-background-color: white;");
            }
        });
    }

    public void setBookData(Book book, String username) {
        this.currentBook = book;
        this.currentUsername = username;

        if (titleLabel != null) {
            titleLabel.setText(book.getTitle());
        }

        loadBookCover();
        loadCurrentProgress();
    }

    public void setProgressUpdateCallback(Consumer<Book> callback) {
        this.progressUpdateCallback = callback;
    }


    private void loadBookCover() {
        if (bookCoverImage == null || currentBook == null) return;

        try {
            String imagePath = currentBook.getCoverImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                String fullResourcePath = "/img/" + imagePath;
                URL imageUrl = getClass().getResource(fullResourcePath);
                if (imageUrl != null) {
                    Image coverImage = new Image(imageUrl.toString());
                    bookCoverImage.setImage(coverImage);
                } else {
                    setDefaultCover();
                }
            } else {
                setDefaultCover();
            }
        } catch (Exception e) {
            setDefaultCover();
        }
    }

    private void setDefaultCover() {
        if (bookCoverImage != null) {
            bookCoverImage.setStyle("-fx-background-color: #dddddd; -fx-border-color: #cccccc;");
        }
    }

    private void loadCurrentProgress() {
        if (currentBook == null || currentUsername == null) return;

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUsername);
            data.put("bookId", currentBook.getBookId());

            Request request = new Request(RequestType.GET_BOOK_PROGRESS, data);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                Book bookWithProgress = (Book) response.getData();
                if (bookWithProgress != null) {
                    updateUIWithProgress(bookWithProgress);
                } else {
                    setDefaultProgress();
                }
            } else {
                setDefaultProgress();
            }

        } catch (Exception e) {
            System.err.println("Error loading progress: " + e.getMessage());
            setDefaultProgress();
        }
    }

    private void updateUIWithProgress(Book book) {
        Platform.runLater(() -> {
            if (currentPageField != null) {
                currentPageField.setText(String.valueOf(book.getCurrentPage()));
            }
            if (totalPagesField != null) {
                totalPagesField.setText(String.valueOf(book.getPages()));
                totalPagesField.setEditable(false);
            }
            if (progressSlider != null) {
                progressSlider.setValue(book.getProgressPercentage());
            }
            if (progressLabel != null) {
                progressLabel.setText(String.format("Progress: %.0f%%", book.getProgressPercentage()));
            }

            setupListeners();
        });
    }

    private void setupListeners() {
        if (currentPageField != null) {
            currentPageField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingFromSlider) {
                    limitCurrentPageInput(newVal);
                    Platform.runLater(() -> updateProgressFromPages());
                }
            });
        }

        if (progressSlider != null) {
            progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingFromText) {
                    updateProgressFromSlider(newVal.doubleValue());
                }
            });
        }
    }

    private void limitCurrentPageInput(String newValue) {
        try {
            if (newValue == null || newValue.trim().isEmpty()) return;

            int currentPage = Integer.parseInt(newValue.trim());
            String totalPagesText = totalPagesField.getText().trim();

            if (!totalPagesText.isEmpty()) {
                int totalPages = Integer.parseInt(totalPagesText);

                if (currentPage > totalPages) {
                    Platform.runLater(() -> {
                        isUpdatingFromSlider = true;
                        currentPageField.setText(String.valueOf(totalPages));
                        currentPageField.positionCaret(currentPageField.getText().length());
                        isUpdatingFromSlider = false;
                    });
                }
            }
        } catch (NumberFormatException e) {
        }
    }

    private void setDefaultProgress() {
        Platform.runLater(() -> {
            if (currentPageField != null) {
                currentPageField.setText("0");
                currentPageField.setStyle("-fx-font-size: 16; -fx-border-color: #658C4C; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
            if (totalPagesField != null) {
                String totalPagesText = (currentBook != null && currentBook.getPages() > 0) ?
                        String.valueOf(currentBook.getPages()) : "300";
                totalPagesField.setText(totalPagesText);
                totalPagesField.setEditable(false);
                totalPagesField.setStyle("-fx-opacity: 0.7; -fx-font-size: 16; -fx-border-color: #658C4C; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
                }
            if (progressSlider != null) progressSlider.setValue(0);
            if (progressLabel != null) progressLabel.setText("Progress: 0%");
            if (saveButton != null) {
                saveButton.setStyle("-fx-background-color: #839174; -fx-font-size: 16; -fx-background-radius: 5px;");
            }

            setupListeners();
        });
    }

    private void updateProgressFromSlider(double sliderValue) {
        if (totalPagesField == null || currentPageField == null) return;

        try {
            isUpdatingFromSlider = true;

            int totalPages = Integer.parseInt(totalPagesField.getText());
            int currentPage = (int) (totalPages * sliderValue / 100.0);

            currentPageField.setText(String.valueOf(currentPage));

            if (progressLabel != null) {
                progressLabel.setText(String.format("Progress: %.0f%%", sliderValue));
            }
        } catch (NumberFormatException e) {
        } finally {
            isUpdatingFromSlider = false;
        }
    }

    private void updateProgressFromPages() {
        Platform.runLater(() -> {
            if (currentPageField == null || totalPagesField == null || progressLabel == null) return;

            try {
                isUpdatingFromText = true;

                String currentPageText = currentPageField.getText().trim();
                String totalPagesText = totalPagesField.getText().trim();

                if (currentPageText.isEmpty() || totalPagesText.isEmpty()) {
                    progressLabel.setText("Progress: 0%");
                    return;
                }

                int currentPage = Integer.parseInt(currentPageText);
                int totalPages = Integer.parseInt(totalPagesText);

                if (totalPages > 0) {
                    double percentage = (currentPage * 100.0) / totalPages;
                    percentage = Math.min(100, Math.max(0, percentage));

                    if (progressSlider != null) {
                        progressSlider.setValue(percentage);
                    }
                    progressLabel.setText(String.format("Progress: %.0f%%", percentage));
                } else {
                    progressLabel.setText("Progress: 0%");
                }
            } catch (NumberFormatException e) {
                progressLabel.setText("Progress: 0%");
            } finally {
                isUpdatingFromText = false;
            }
        });
    }

    @FXML
    public void onSaveClicked(ActionEvent event) {
        try {
            int currentPage = Integer.parseInt(currentPageField.getText());

            if (currentPage < 0) {
                showAlert("Invalid Input", "Please enter valid page numbers.");
                return;
            }

            if (currentPage > currentBook.getPages()) {
                currentPage = currentBook.getPages();
                currentPageField.setText(String.valueOf(currentPage));
            }

            currentBook.setCurrentPage(currentPage);

            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUsername);
            data.put("bookId", currentBook.getBookId());
            data.put("currentPage", currentPage);

            Request request = new Request(RequestType.UPDATE_PROGRESS, data);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                System.out.println("Progress updated successfully");

                if (progressUpdateCallback != null) {
                    progressUpdateCallback.accept(currentBook); // Przekaż zaktualizowany Book
                }

                closeWindow();
            } else {
                showAlert("Error", "Failed to update progress: " + response.getData());
            }

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter valid numbers for pages.");
        } catch (Exception e) {
            System.err.println("Error saving progress: " + e.getMessage());
            showAlert("Error", "An error occurred while saving progress.");
        }
    }

    private void updateReadingStatus(int bookId, String username, String newStatus) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("bookId", bookId);
            data.put("status", newStatus);

            Request request = new Request(RequestType.UPDATE_READING_STATUS, data);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                System.out.println("Reading status automatically updated to: " + newStatus);
            } else {
                System.err.println("Error updating reading status: " + response.getData());
            }
        } catch (Exception e) {
            System.err.println("Exception updating reading status: " + e.getMessage());
        }
    }

    @FXML
    public void saveButtonOnAction(ActionEvent event) {
        onSaveClicked(event);
    }

    @FXML
    public void onCancelClicked(ActionEvent event) {
        closeWindow();
    }

    @FXML
    public void closeButtonOnAction(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = null;

        if (cancelButton != null && cancelButton.getScene() != null) {
            stage = (Stage) cancelButton.getScene().getWindow();
        } else if (saveButton != null && saveButton.getScene() != null) {
            stage = (Stage) saveButton.getScene().getWindow();
        } else if (progressSlider != null && progressSlider.getScene() != null) {
            stage = (Stage) progressSlider.getScene().getWindow();
        } else if (currentPageField != null && currentPageField.getScene() != null) {
            stage = (Stage) currentPageField.getScene().getWindow();
        }

        if (stage != null) {
            stage.close();
        } else {
            System.err.println("Cannot find stage to close");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}