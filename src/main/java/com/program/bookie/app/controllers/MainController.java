package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class MainController implements Initializable {

    //MENU
    @FXML
    private Button closeButton,miniButton,homeButton,shelfButton,statisticsButton,searchButton;
    //OTHERS
    @FXML
    private Label welcomeLabel;

    @FXML
    private HBox booksContainer;

    @FXML
    private VBox searchBox;

    @FXML
    private Pane homePane, searchPane, bookDetailsPane;

    @FXML
    private Label detailsTitle, detailsAuthor, detailsRatings,detailsReviews,detailsDescription, detailsAvgRating,ratingStatusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ImageView coverBookDetails,detailsStarY1,detailsStarY2,detailsStarY3,detailsStarY4,detailsStarY5,detailsStarG1,detailsStarG2,detailsStarG3,detailsStarG4,detailsStarG5,userStar1,userStar2,userStar3,userStar4,userStar5;

    @FXML
    private ComboBox detailsStatusCombo;

    @FXML
    private Button editDetailsButton;

    private Book currentBookDetails;

    private Client client = Client.getInstance();
    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome " + user.getUsername() + "!");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        setHover(homeButton);
        setHover(statisticsButton);
        setHover(shelfButton);

        /// shutdownHook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client != null) {
                client.disconnect();
            }
        }));
    }

    //MENU
    public void closeButtonOnAction(ActionEvent actionEvent) {
        if (client != null) {
            System.out.println("Disconnecting client...");
            client.disconnect();
        }
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    public void miniButtonOnAction(ActionEvent actionEvent) {
        Stage stage = (Stage) miniButton.getScene().getWindow();
        stage.setIconified(true);
    }

    public void setHover(Button button) {
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #54664D;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #839174;"));
    }

    //HOMEPAGE
    public void loadTopRatedBooks() {
        try {
            Request request = new Request(RequestType.GET_TOP_BOOKS, 4);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                @SuppressWarnings("unchecked")
                List<Book> topBooks = (List<Book>) response.getData();
                displayBooks(topBooks);
            } else {
                System.err.println("Błąd pobierania książek: " + response.getData());
            }
        } catch (Exception e) {
            System.err.println("Błąd połączenia: " + e.getMessage());
        }
    }

    private void displayBooks(List<Book> books) {
        booksContainer.getChildren().clear();
        booksContainer.setSpacing(20);
        booksContainer.setAlignment(Pos.CENTER);

        for (Book book : books) {
            VBox bookCard = createBookCard(book);
            booksContainer.getChildren().add(bookCard);
        }
    }

    private VBox createBookCard(Book book) {
        VBox bookCard = new VBox();
        bookCard.setAlignment(Pos.CENTER);
        bookCard.setSpacing(10);
        bookCard.setPadding(new Insets(15));
        bookCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        bookCard.setPrefWidth(200);
        bookCard.setPrefHeight(270);

        // Okładka książki
        ImageView coverImageView = new ImageView();
        coverImageView.setFitWidth(120);
        coverImageView.setFitHeight(160);
        coverImageView.setPreserveRatio(true);

        try {
            String imagePath = book.getCoverImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                String fullResourcePath = "/img/" + imagePath;

                URL imageUrl = getClass().getResource(fullResourcePath);
                if (imageUrl != null) {
                    Image coverImage = new Image(imageUrl.toString());
                    coverImageView.setImage(coverImage);
                    System.out.println("Successfully loaded image: " + fullResourcePath);
                } else {
                    System.err.println("Image not found in resources: " + fullResourcePath);
                    setDefaultCoverImage(coverImageView);
                }
            } else {
                setDefaultCoverImage(coverImageView);
            }
        } catch (Exception e) {
            setDefaultCoverImage(coverImageView);
        }

        // Tytuł książki
        Label titleLabel = new Label(book.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-text-alignment: center;");

        // Autor
        Label authorLabel = new Label("by " + book.getAuthor());
        authorLabel.setFont(Font.font("System", 12));
        authorLabel.setStyle("-fx-text-fill: #666666;");
        authorLabel.setWrapText(true);
        authorLabel.setMaxWidth(180);
        authorLabel.setAlignment(Pos.CENTER);

        // Ocena
        Label ratingLabel = new Label();
        if (book.getRatingCount() > 0) {
            ratingLabel.setText(String.format("★ %.1f (%d)", book.getAverageRating(), book.getRatingCount()));
        } else {
            ratingLabel.setText("No ratings yet");
        }
        ratingLabel.setFont(Font.font("System", 11));
        ratingLabel.setStyle("-fx-text-fill: #ffa500;");

        bookCard.getChildren().addAll(coverImageView, titleLabel, authorLabel, ratingLabel);


        bookCard.setOnMouseEntered(e -> {
            bookCard.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        });

        bookCard.setOnMouseExited(e -> {
            bookCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        });


        bookCard.setOnMouseClicked(e -> {
            System.out.println("Clicked on book: " + book.getTitle());
            showBookDetails(book);
        });

        return bookCard;
    }

    private void setDefaultCoverImage(ImageView imageView) {
        imageView.setStyle("-fx-background-color: #dddddd; -fx-border-color: #cccccc; -fx-border-width: 1;");
    }

    //SHELVES

    //SEARCH

    public void showSearchResults(List<Book> books) {
        searchBox.getChildren().clear();

        for (Book book : books) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/searchBokk.fxml"));
                HBox searchResult = loader.load();

                SearchController controller = loader.getController();
                controller.setData(book, currentUser.getUsername());
                controller.setMainController(this);

                searchBox.getChildren().add(searchResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onSearchClicked() {
        String searchTerm = searchField.getText();
        if (searchTerm == null || searchTerm.isBlank()) return;

        try {
            Request request = new Request(RequestType.SEARCH_BOOK, searchTerm);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                List<Book> results = (List<Book>) response.getData();

                showSearchResults(results);
                searchPane.setVisible(true);
                homePane.setVisible(false);
                bookDetailsPane.setVisible(false);
            } else {
                System.err.println("Search failed: " + response.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }}

    public void onHomeClicked()
    {
        loadTopRatedBooks();
        homePane.setVisible(true);
        searchPane.setVisible(false);
        bookDetailsPane.setVisible(false);

    }

    //BOOK DETAILS
    public void showBookDetails(Book book) {
        if (book == null) return;

        currentBookDetails=book;
        if (detailsTitle != null) {
            detailsTitle.setText(book.getTitle());
        }

        if (detailsAuthor != null) {
            detailsAuthor.setText("by " + book.getAuthor());
        }

        if (detailsAvgRating != null) {
            if (book.getRatingCount() > 0) {

                detailsAvgRating.setText(String.format("%.2f", book.getAverageRating()));
            } else {
                detailsAvgRating.setText("No ratings yet");
            }
        }

        if (detailsRatings != null) {
            detailsRatings.setText(book.getRatingCount() + " ratings");
        }

        if (detailsReviews != null) {
            detailsReviews.setText(book.getRatingCount() + " reviews");
        }

        if (detailsDescription != null) {
            String description = book.getDescription();
            if (description != null && !description.trim().isEmpty()) {
                detailsDescription.setText(description);
                detailsDescription.setWrapText(true);
            } else {
                detailsDescription.setText("No description available.");
            }
        }

        if (coverBookDetails != null) {
            try {
                String imagePath = book.getCoverImagePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    String fullResourcePath = "/img/" + imagePath;
                    URL imageUrl = getClass().getResource(fullResourcePath);
                    if (imageUrl != null) {
                        Image coverImage = new Image(imageUrl.toString());
                        coverBookDetails.setImage(coverImage);
                    } else {
                        setDefaultDetailsCoverImage();
                    }
                } else {
                    setDefaultDetailsCoverImage();
                }
            } catch (Exception e) {
                setDefaultDetailsCoverImage();
                System.err.println("Error loading book details cover: " + e.getMessage());
            }
        }


        updateStarRatingD(book.getAverageRating());

        //user status
        initializeBookDetailsComboBox();
        loadBookDetailsReadingStatus();
        loadBookDetailsUserRating();


        homePane.setVisible(false);
        searchPane.setVisible(false);
        bookDetailsPane.setVisible(true);
    }

    private void setDefaultDetailsCoverImage() {
        if (coverBookDetails != null) {
            coverBookDetails.setStyle("-fx-background-color: #dddddd; -fx-border-color: #cccccc; -fx-border-width: 1;");
            coverBookDetails.setImage(null);
        }
    }

    private void updateStarRatingD(double rating) {
        ImageView[] yellowStars = {detailsStarY1, detailsStarY2, detailsStarY3, detailsStarY4, detailsStarY5};
        ImageView[] grayStars = {detailsStarG1, detailsStarG2, detailsStarG3, detailsStarG4, detailsStarG5};

        for (ImageView grayStar : grayStars) {
            if (grayStar != null) {
                grayStar.setVisible(true);
            }
        }

        int fullStars = (int) Math.floor(rating);
        double partialStar = rating - fullStars;

        for (int i = 0; i < yellowStars.length; i++) {
            if (yellowStars[i] != null) {
                if (i < fullStars) {
                    yellowStars[i].setVisible(true);
                    yellowStars[i].setClip(null);
                } else if (i == fullStars && partialStar > 0) {
                    // Przycinanie gwiazdki
                    yellowStars[i].setVisible(true);


                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
                    clip.setWidth(yellowStars[i].getFitWidth() * partialStar);
                    clip.setHeight(yellowStars[i].getFitHeight());
                    yellowStars[i].setClip(clip);
                } else {
                    yellowStars[i].setVisible(false);
                }
            }
        }

        System.out.println("Updated star rating to: " + rating + " (Full stars: " + fullStars + ", Partial: " + partialStar + ")");
    }


    //DETAILS USER SECTION
    private final String[] statuses = {"Want to read", "Currently reading", "Read"};
    private final String WANT_TO_READ = "Want to read";
    private boolean isUpdatingStatus = false;

    private void initializeBookDetailsComboBox() {
        if (detailsStatusCombo != null) {
            detailsStatusCombo.getItems().clear();
            detailsStatusCombo.getItems().addAll(statuses);

            detailsStatusCombo.setOnAction(e -> {
                if (!isUpdatingStatus) {
                    String selectedStatus = (String) detailsStatusCombo.getSelectionModel().getSelectedItem();
                    if (selectedStatus != null) {
                        updateBookDetailsReadingStatus(selectedStatus);
                    }
                }
            });
        }
    }

    private void loadBookDetailsReadingStatus() {
        if (currentBookDetails == null || currentUser == null) return;

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUser.getUsername());
            data.put("bookId", currentBookDetails.getBookId());

            Request request = new Request(RequestType.GET_READING_STATUS, data);
            Response response = client.sendRequest(request);

            isUpdatingStatus = true;

            if (response.getType() == ResponseType.SUCCESS) {
                String status = (String) response.getData();
                if (status == null || status.trim().isEmpty()) {
                    detailsStatusCombo.getSelectionModel().select(WANT_TO_READ);
                    setBookDetailsComboBoxStyle(true);
                } else {
                    detailsStatusCombo.getSelectionModel().select(status);
                    setBookDetailsComboBoxStyle(false);
                }
            } else {
                System.err.println("Error loading reading status: " + response.getData());
                detailsStatusCombo.getSelectionModel().select(WANT_TO_READ);
                setBookDetailsComboBoxStyle(true);
            }

        } catch (Exception e) {
            System.err.println("Exception loading reading status: " + e.getMessage());
            detailsStatusCombo.getSelectionModel().select(WANT_TO_READ);
            setBookDetailsComboBoxStyle(true);
        } finally {
            isUpdatingStatus = false;
        }
    }

    private void updateBookDetailsReadingStatus(String selectedStatus) {
        if (currentBookDetails == null || currentUser == null) return;

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUser.getUsername());
            data.put("bookId", currentBookDetails.getBookId());
            data.put("status", selectedStatus);

            Request request = new Request(RequestType.UPDATE_READING_STATUS, data);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                System.out.println("Reading status updated successfully to: " + selectedStatus);

                if ("Read".equals(selectedStatus)) {
                    openReviewWindow(currentBookDetails, currentUser.getUsername(), false);
                }

                setBookDetailsComboBoxStyle(false);
            } else {
                System.err.println("Error updating reading status: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("Exception updating reading status: " + e.getMessage());
        }
    }

    private void setBookDetailsComboBoxStyle(boolean isNewStatus) {
        if (detailsStatusCombo == null) return;

        detailsStatusCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
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

        detailsStatusCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
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

    private void loadBookDetailsUserRating() {
        if (currentBookDetails == null || currentUser == null) return;

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", currentUser.getUsername());
            data.put("bookId", currentBookDetails.getBookId());

            Request request = new Request(RequestType.GET_USER_RATING, data);
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                Integer rating = (Integer) response.getData();
                displayBookDetailsUserRating(rating != null ? rating : 0);
                updateRatingStatusLabel(rating);
            } else {
                displayBookDetailsUserRating(0);
                updateRatingStatusLabel(null);
            }

        } catch (Exception e) {
            System.err.println("Exception loading user rating: " + e.getMessage());
            displayBookDetailsUserRating(0);
            updateRatingStatusLabel(null);
        }
    }

    private void displayBookDetailsUserRating(int rating) {
        ImageView[] stars = {userStar1, userStar2, userStar3, userStar4, userStar5};

        for (int i = 0; i < stars.length; i++) {
            if (stars[i] != null) {
                boolean filled = i < rating;
                setBookDetailsStarImage(stars[i], filled);
            }
        }
    }

    private void setBookDetailsStarImage(ImageView star, boolean filled) {
        try {
            String imagePath = filled ? "/img/star.png" : "/img/star2.png";
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            star.setImage(image);
        } catch (Exception e) {
            System.err.println("Error loading star image: " + e.getMessage());
        }
    }

    private void updateRatingStatusLabel(Integer rating) {
        if (ratingStatusLabel != null) {
            if (rating == null || rating == 0) {
                ratingStatusLabel.setText("Not rated yet");
                if (editDetailsButton != null) {
                    editDetailsButton.setVisible(false);
                }
            } else {
                ratingStatusLabel.setText("Edit rating/review");
                if (editDetailsButton != null) {
                    editDetailsButton.setVisible(true);
                }
            }
        }
    }

    //EDIT OR ADD REVIEW FROM DETAILS
    private void openReviewWindow(Book book, String username, boolean editMode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/review.fxml"));
            AnchorPane reviewPane = loader.load();

            ReviewController controller = loader.getController();
            controller.setBookData(book, username, editMode);

            Stage reviewStage = new Stage();
            reviewStage.setTitle(editMode ? "Edit Review" : "Add Review");
            reviewStage.initStyle(StageStyle.UNDECORATED);
            reviewStage.setScene(new Scene(reviewPane, 480, 360));
            reviewStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icon.png"))));
            reviewStage.setResizable(false);

            // Po zamknięciu okna review, odśwież dane
            reviewStage.setOnHidden(e -> {
                // Odśwież rating status label
                loadBookDetailsUserRating();
            });

            reviewStage.show();

        } catch (Exception e) {
            System.err.println("Error opening review window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onEditRatingButtonClicked() {
        if (currentBookDetails != null && currentUser != null) {
            openReviewWindow(currentBookDetails, currentUser.getUsername(), true);
        }
    }




}




