package com.program.bookie.app.controllers;

import com.program.bookie.models.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.program.bookie.models.ResponseType.*;
import com.program.bookie.client.Client;
import com.program.bookie.models.ResponseType.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class SearchController {

    private Client client = Client.getInstance();

    @FXML
    private ImageView bookCover,star1,star2,star3,star4,star5;
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

    @FXML
    private HBox mainPane;

    private boolean isUpdating = false;

    private final String[] statuses = {"Want to read", "Currently reading", "Read"};
    private final String WANT_TO_READ = "Want to read";

    private String currentUsername;
    private int currentBookId;
    private Book currentBook;
    private MainController mainController; // Controller glownego panelu (przesylanie informacji o kliknieciu)

    public void setData(Book book, String username) {
        setBasicData(book, username);
        loadAdditionalDataAsync();
    }

    public void loadAdditionalDataAsync() {
        // Załaduj okładkę
        loadBookCoverAsync(currentBook);

        // Załaduj rating użytkownika
        loadUserRatingAsync();

        // Załaduj status czytania
        loadReadingStatusAsync();
    }


    public void setBasicData(Book book, String username) {
        this.currentUsername = username;
        this.currentBookId = book.getBookId();
        this.currentBook = book;

        // Ustaw tylko podstawowe informacje tekstowe (bez komunikacji z serwerem)
        titleLabel.setText(book.getTitle());
        authorLabel.setText("by " + book.getAuthor());
        avgRatingLabel.setText(String.format("★%.1f", book.getAverageRating()) + " avg rating");
        ratingCountLabel.setText(book.getRatingCount() + " ratings");
        publicationYearLabel.setText("published in " + book.getPublicationYear());

        // Ustaw placeholder dla okładki
        bookCover.setFitWidth(150);
        bookCover.setFitHeight(200);
        bookCover.setPreserveRatio(true);
        bookCover.setSmooth(true);
        bookCover.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 1;");

        // Ustaw placeholder dla gwiazdek (puste)
        ImageView[] stars = {star1, star2, star3, star4, star5};
        for (ImageView star : stars) {
            if (star != null) {
                setStarImage(star, false); // Wszystkie puste na start
            }
        }

        // Ustaw placeholder dla ComboBox
        initializeComboBox();
        statusComboBox.getSelectionModel().select(WANT_TO_READ);
        statusComboBox.setDisable(true); // Zablokuj na czas ładowania

        setupClickablePanel();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void setupClickablePanel() {
        if (mainPane != null) {
            mainPane.setStyle(mainPane.getStyle() + "; -fx-cursor: hand;");

            mainPane.setOnMouseEntered(e -> {
                mainPane.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 15; -fx-cursor: hand;");
            });

            mainPane.setOnMouseExited(e -> {
                mainPane.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 15; -fx-cursor: hand;");
            });

            mainPane.setOnMouseClicked(e -> {

                if (e.getTarget() == statusComboBox ||
                        statusComboBox.isShowing() ||
                        isClickOnComboBox(e.getTarget())) {
                    return;
                }

                if (mainController != null && currentBook != null) {
                    mainController.showBookDetails(currentBook);
                } else {
                    System.out.println("Clicked on book: " + (currentBook != null ? currentBook.getTitle() : "Unknown"));
                }
            });
        }
    }

    private boolean isClickOnComboBox(Object target) {
        if (target instanceof javafx.scene.Node) {
            javafx.scene.Node node = (javafx.scene.Node) target;
            javafx.scene.Node parent = node.getParent();

            while (parent != null) {
                if (parent == statusComboBox) {
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    /**
     * Nowa, zoptymalizowana metoda ładowania okładki z cache
     */
    private void loadBookCoverAsync(Book book) {
        String imagePath = book.getCoverImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            setDefaultCover();
            return;
        }

        // Sprawdź cache najpierw (synchronicznie - bardzo szybko)
        Image cachedImage = client.getImageFX(imagePath);
        if (cachedImage != null) {
            setImageWithViewport(cachedImage);
            bookCover.setStyle(""); // Usuń placeholder
            return;
        }

        // Załaduj z serwera asynchronicznie (tylko jeśli nie ma w cache)
        Request imageRequest = new Request(RequestType.GET_IMAGE, imagePath);

        client.executeAsyncWithData(imageRequest, new Client.ResponseHandler() {
            @Override
            public void handle(Response response) {
                if (response.getType() == ResponseType.SUCCESS) {
                    try {
                        ImageData imageData = (ImageData) response.getData();
                        if (imageData != null && imageData.getImageBytes() != null) {
                            Image serverImage = new Image(new java.io.ByteArrayInputStream(imageData.getImageBytes()));
                            setImageWithViewport(serverImage);
                            bookCover.setStyle(""); // Usuń placeholder
                        } else {
                            loadLocalCover(imagePath);
                        }
                    } catch (Exception e) {
                        loadLocalCover(imagePath);
                    }
                } else {
                    loadLocalCover(imagePath);
                }
            }

            @Override
            public void handleError(Exception e) {
                Platform.runLater(() -> loadLocalCover(imagePath));
            }
        });
    }

    /**
     * Ustawia obraz z odpowiednim viewport (przycinanie)
     */
    private void setImageWithViewport(Image image) {
        if (bookCover == null || image == null) return;

        bookCover.setImage(image);

        // Ustaw viewport dla lepszego wyświetlania (przytnij do proporcji okładki)
        double fitWidth = 150;
        double fitHeight = 200;

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

    /**
     * Fallback do lokalnych zasobów
     */
    private void loadLocalCover(String imagePath) {
        try {
            String fullResourcePath = "/img/" + imagePath;
            URL imageUrl = getClass().getResource(fullResourcePath);
            if (imageUrl != null) {
                Image localImage = new Image(imageUrl.toString());
                setImageWithViewport(localImage);
                System.out.println("✅ Search cover loaded locally: " + fullResourcePath);
            } else {
                System.err.println("Local image not found: " + fullResourcePath);
                setDefaultCover();
            }
        } catch (Exception e) {
            System.err.println("Error loading local cover: " + e.getMessage());
            setDefaultCover();
        }
    }

    /**
     * Ustawia domyślną okładkę
     */
    private void setDefaultCover() {
        if (bookCover != null) {
            bookCover.setImage(null);
            bookCover.setStyle("-fx-background-color: #dddddd; -fx-border-color: #cccccc; -fx-border-width: 1;");
            System.out.println("🔄 Using default cover for search result");
        }
    }



    private void initializeComboBox() {
        statusComboBox.getItems().clear();
        statusComboBox.getItems().addAll(statuses);

        statusComboBox.setOnAction(e -> {
            if (!isUpdating) {
                String selectedStatus = statusComboBox.getSelectionModel().getSelectedItem();
                if (selectedStatus != null) {
                    updateReadingStatus(selectedStatus);
                }
            }
        });
    }

    private void loadUserRatingAsync() {
        Map<String, Object> data = new HashMap<>();
        data.put("username", currentUsername);
        data.put("bookId", currentBookId);

        Request request = new Request(RequestType.GET_USER_RATING, data);

        client.executeAsyncWithData(request, new Client.ResponseHandler() {
            @Override
            public void handle(Response response) {
                if (response.getType() == ResponseType.SUCCESS) {
                    Integer rating = (Integer) response.getData();
                    displayUserRating(rating != null ? rating : 0);
                } else {
                    displayUserRating(0);
                    System.err.println("Error loading user rating: " + response.getData());
                }
            }

            @Override
            public void handleError(Exception e) {
                System.err.println("Exception loading user rating: " + e.getMessage());
                Platform.runLater(() -> displayUserRating(0));
            }
        });
    }


    // Funkcja do wyświetlania gwiazdek na podstawie oceny
    private void displayUserRating(int rating) {
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


    private void loadReadingStatusAsync() {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUsername);
            data.put("bookId", currentBookId);

            Request request = new Request(RequestType.GET_READING_STATUS, data);

            client.executeAsyncWithData(request, new Client.ResponseHandler() {
                @Override
                public void handle(Response response) {
                    isUpdating = true;
                    try {
                        if (response.getType() == ResponseType.SUCCESS) {
                            String status = (String) response.getData();
                            if (status == null || status.trim().isEmpty()) {
                                statusComboBox.getSelectionModel().select(WANT_TO_READ);
                                setComboBoxStyle(true);
                            } else {
                                statusComboBox.getSelectionModel().select(status);
                                setComboBoxStyle(false);
                            }
                        } else {
                            statusComboBox.getSelectionModel().select(WANT_TO_READ);
                            setComboBoxStyle(true);
                        }

                        // WAŻNE: Odblokuj ComboBox po załadowaniu
                        statusComboBox.setDisable(false);

                    } finally {
                        isUpdating = false;
                    }
                }

                @Override
                public void handleError(Exception e) {
                    Platform.runLater(() -> {
                        isUpdating = true;
                        statusComboBox.getSelectionModel().select(WANT_TO_READ);
                        setComboBoxStyle(true);
                        statusComboBox.setDisable(false); // Odblokuj nawet przy błędzie
                        isUpdating = false;
                    });
                }
            });
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

                if ("Read".equals(selectedStatus)) {
                    openReviewWindow(currentBook, currentUsername);
                }

                if (mainController != null) {
                    mainController.refreshShelvesIfNeeded();
                }

                setComboBoxStyle(false);

            }

            else {
                System.err.println("Error updating reading status: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("Exception updating reading status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setComboBoxStyle(boolean isNewStatus) {
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

    //ADD REVIEW

    private void openReviewWindow(Book book, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/review.fxml"));
            AnchorPane reviewPane = loader.load();

            ReviewController controller = loader.getController();
            controller.setBookData(book, username);

            Stage reviewStage = new Stage();
            reviewStage.initStyle(StageStyle.UNDECORATED);
            reviewStage.setScene(new Scene(reviewPane, 480, 360));
            reviewStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icon.png"))));
            reviewStage.setResizable(false);

            // Po zamknięciu okna review, odśwież dane
            reviewStage.setOnHidden(e -> {
                loadUserRatingAsync();
            });

            reviewStage.show();

        } catch (Exception e) {
            System.err.println("Error opening review window: " + e.getMessage());
            e.printStackTrace();
        }
    }
}