package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.client.NotificationService;
import com.program.bookie.models.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.stage.StageStyle;
import javafx.application.Platform;

import java.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Collectors;

import javafx.util.Duration;


public class MainController implements Initializable {

    //MENU
    @FXML
    private Button closeButton, miniButton, homeButton, shelfButton, statisticsButton, searchButton;
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
    private Label detailsTitle, detailsAuthor, detailsRatings, detailsReviews, detailsDescription, detailsAvgRating, ratingStatusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ImageView coverBookDetails, detailsStarY1, detailsStarY2, detailsStarY3, detailsStarY4, detailsStarY5, detailsStarG1, detailsStarG2, detailsStarG3, detailsStarG4, detailsStarG5, userStar1, userStar2, userStar3, userStar4, userStar5;

    @FXML
    private ComboBox detailsStatusCombo;

    @FXML
    private Button editDetailsButton;

    private Book currentBookDetails;

    @FXML
    private Button userButton;
    @FXML
    private VBox userDropdown;
    @FXML
    private Label userGreeting;
    @FXML
    private Button accountSettingsButton;
    @FXML
    private Button logoutButton;

    @FXML
    private Label quoteLabel;

    @FXML
    private Pane statisticsPane;
    @FXML
    private VBox reviewsContainer;

    @FXML private Button bellButton;
    @FXML private StackPane notificationBadge;
    @FXML private Label countNLabel;
    @FXML private VBox notificationDropdown;
    @FXML private VBox notificationsList;

    private boolean isUserMenuVisible = false;
    private NotificationService notificationService;
    private boolean isNotificationMenuVisible = false;

    private Client client = Client.getInstance();
    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome " + user.getUsername() + "!");
        }
        loadRandomQuote();
        if (notificationService != null) {
            notificationService.start(user.getUsername());
            System.out.println("Notification service started for: " + user.getUsername());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        client.clearImageCache();
        notificationService = NotificationService.getInstance();
        System.out.println("NotificationService started");
        setHover(homeButton);
        setHover(statisticsButton);
        setHover(shelfButton);

        System.out.println("bellButton: " + bellButton);
        System.out.println("notificationBadge: " + notificationBadge);
        System.out.println("notificationCountLabel: " + countNLabel);

        // Set up bell click handler
        if (bellButton != null) {
            bellButton.setOnAction(event -> {
                System.out.println("Bell button clicked!");
                toggleNotificationMenu();
            });
        } else {
            System.err.println("bellButton is NULL!");
        }

        /// shutdownHook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client != null) {
                client.disconnect();
            }
        }));

        Platform.runLater(() -> {
            if (userButton.getScene() != null) {
                userButton.getScene().setOnMouseClicked(event -> {
                    boolean clickedOnUser = isClickOnUserMenu(event.getTarget());
                    boolean clickedOnNotification = isClickOnNotificationMenu(event.getTarget());

                    if (!clickedOnUser && isUserMenuVisible) {
                        hideUserMenu();
                    }

                    if (!clickedOnNotification && isNotificationMenuVisible) {
                        hideNotificationMenu();
                    }
                });
            }
        });

        if (searchField != null) {
            searchField.setOnMouseClicked(event -> {
                if (isUserMenuVisible) {
                    hideUserMenu();
                }
            });
        }

        // Hover effects dla przycisków w menu użytkownika
        if (accountSettingsButton != null) {
            accountSettingsButton.setOnMouseEntered(e ->
                    accountSettingsButton.setStyle("-fx-background-color: #839174; -fx-alignment: center-left; -fx-padding: 10 16; -fx-text-fill: white; -fx-border-color: transparent;"));
            accountSettingsButton.setOnMouseExited(e ->
                    accountSettingsButton.setStyle("-fx-background-color: transparent; -fx-alignment: center-left; -fx-padding: 10 16; -fx-text-fill: #615252; -fx-border-color: transparent;"));
        }

        if (logoutButton != null) {
            logoutButton.setOnMouseEntered(e ->
                    logoutButton.setStyle("-fx-background-color: #839174; -fx-alignment: center-left; -fx-padding: 10 16; -fx-text-fill: white; -fx-border-color: transparent;"));
            logoutButton.setOnMouseExited(e ->
                    logoutButton.setStyle("-fx-background-color: transparent; -fx-alignment: center-left; -fx-padding: 10 16; -fx-text-fill: #615252; -fx-border-color: transparent;"));
        }

        if (notificationDropdown != null) {
            notificationDropdown.setVisible(false);

        }

        if (notificationService != null) {
            notificationService.getNotifications().addListener((ListChangeListener<INotification>) change -> {
                while (change.next()) {
                    if (change.wasAdded() || change.wasReplaced()) {
                        // Lista się zmieniła - odśwież wyświetlanie jeśli menu jest otwarte
                        if (isNotificationMenuVisible) {
                            Platform.runLater(this::displayNotifications);
                        }
                    }
                }
            });

        }
        setupNotificationBindings();



    }
    private boolean isClickOnNotificationMenu(Object target) {
        if (target instanceof javafx.scene.Node) {
            javafx.scene.Node node = (javafx.scene.Node) target;

            if (node == bellButton || isChildOf(node, bellButton)) {
                return true;
            }

            if (node == notificationDropdown || isChildOf(node, notificationDropdown)) {
                return true;
            }
        }
        return false;
    }


    //MENU
    public void closeButtonOnAction(ActionEvent actionEvent) {
        if (notificationService != null) {
            notificationService.stop();
        }
        if (client != null) {
            System.out.println("Disconnecting client...");
            client.clearImageCache();
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

    public void toggleUserMenu(ActionEvent event) {
        isUserMenuVisible = !isUserMenuVisible;
        userDropdown.setVisible(isUserMenuVisible);



        if (isUserMenuVisible && currentUser != null) {
            hideNotificationMenu();
            userGreeting.setText("Hi " + currentUser.getUsername() + "!");
        }
    }

    public void onAccountSettingsClicked(ActionEvent event) {
        System.out.println("Account settings clicked - funkcja do zaimplementowania w przyszłości");
        hideUserMenu();
    }

    public void onLogoutClicked(ActionEvent event) {
        try {
            if (notificationService != null) {
                notificationService.stop();
            }
            if (client != null) {
                System.out.println("Logging out user: " + currentUser.getUsername());
                client.clearImageCache();
                client.disconnect();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/login.fxml"));
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Bookie");
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.setScene(new Scene(root, 520, 400));
            loginStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icon.png"))));

            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();
            loginStage.show();

        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onClearNotificationsClicked() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Notifications");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to clear all notifications?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            notificationService.clearAllNotifications();
            hideNotificationMenu();
        }
    }


    private void hideUserMenu() {
        if (isUserMenuVisible) {
            isUserMenuVisible = false;
            userDropdown.setVisible(false);
        }
    }

    private void clearSearchField() {
        if (searchField != null) {
            searchField.clear();
        }
    }

    // Sprawdź czy kliknięto na przycisk użytkownika lub menu
    private boolean isClickOnUserMenu(Object target) {
        if (target instanceof javafx.scene.Node) {
            javafx.scene.Node node = (javafx.scene.Node) target;

            // Sprawdź czy kliknięto na przycisk użytkownika
            if (node == userButton || isChildOf(node, userButton)) {
                return true;
            }

            // Sprawdź czy kliknięto w menu dropdown
            if (node == userDropdown || isChildOf(node, userDropdown)) {
                return true;
            }
        }
        return false;
    }

    private boolean isChildOf(javafx.scene.Node node, javafx.scene.Node parent) {
        javafx.scene.Node current = node.getParent();
        while (current != null) {
            if (current == parent) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    //CYTATY
    private void loadRandomQuote() {
        try {
            Request request = new Request(RequestType.GET_RANDOM_QUOTE, null);
            Response response = client.sendRequest(request);


            System.out.println("Response type: " + response.getType());
            System.out.println("Response data class: " + response.getData().getClass().getName());

            if (response.getType() == ResponseType.SUCCESS) {
                Object data = response.getData();
                if (data instanceof Quote) {
                    Quote quote = (Quote) data;
                    updateQuoteLabel(quote);
                } else {
                    System.err.println("Expected Quote but got: " + data.getClass().getName());
                    setDefaultQuote();
                }
            }
        } catch (Exception e) {
            System.err.println("Exception loading quote: " + e.getMessage());
            e.printStackTrace();
            setDefaultQuote();
        }
    }

    private void updateQuoteLabel(Quote quote) {
        if (quoteLabel != null && quote != null) {
            Platform.runLater(() -> {
                String formattedQuote = quote.getFormattedQuote();
                quoteLabel.setText(formattedQuote);

                // Dostosuj rozmiar czcionki w zależności od długości cytatu
                adjustQuoteFontSize(formattedQuote.length());
            });
        }
    }

    private void adjustQuoteFontSize(int quoteLength) {
        if (quoteLabel == null) return;

        double fontSize;

        if (quoteLength <= 60) {
            fontSize = 20.0;
        } else if (quoteLength <= 90) {
            fontSize = 18.0;
        } else if (quoteLength <= 130) {
            fontSize = 16.0;
        } else if (quoteLength <= 180) {
            fontSize = 15.0;
        } else {
            fontSize = 14.0;
        }

        Platform.runLater(() -> {
            quoteLabel.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: white; -fx-font-family: 'Bookman Old Style Italic'; -fx-text-alignment: center;");
            quoteLabel.setWrapText(true);
            quoteLabel.setMaxWidth(610);
            quoteLabel.setPrefWidth(610);

            if (quoteLength <= 60) {
                quoteLabel.setMinHeight(24);
                quoteLabel.setPrefHeight(30);
                quoteLabel.setMaxHeight(35);
                quoteLabel.setTranslateY(-8);
            } else {
                quoteLabel.setMinHeight(45);
                quoteLabel.setPrefHeight(55);
                quoteLabel.setMaxHeight(65);
                quoteLabel.setTranslateY(-20);
            }

            quoteLabel.setAlignment(javafx.geometry.Pos.CENTER);
        });

        System.out.println("Adjusted quote font size to: " + fontSize + "px for quote length: " + quoteLength + " (max 2 lines)");
    }

    private void setDefaultQuote() {
        if (quoteLabel != null) {
            Platform.runLater(() -> {
                String defaultText = "\"Welcome to Bookie - your personal reading companion!\"";
                quoteLabel.setText(defaultText);
                adjustQuoteFontSize(defaultText.length());
            });
        }
    }

    //HOMEPAGE
    public void loadTopRatedBooks() {
        Request request = new Request(RequestType.GET_TOP_BOOKS, 4);

        client.executeAsyncWithData(request, new Client.ResponseHandler() {
            @Override
            public void handle(Response response) {
                if (response.getType() == ResponseType.SUCCESS) {
                    @SuppressWarnings("unchecked")
                    List<Book> topBooks = (List<Book>) response.getData();
                    displayBooks(topBooks);
                    System.out.println("Top books loaded successfully!");
                } else {
                    System.err.println("Błąd pobierania książek: " + response.getData());
                }
            }

            @Override
            public void handleError(Exception e) {
                System.err.println("Błąd połączenia: " + e.getMessage());
            }
        });
    }

    private void displayBooks(List<Book> books) {
        booksContainer.getChildren().clear();
        booksContainer.setSpacing(20);
        booksContainer.setAlignment(Pos.CENTER);

        // Preload obrazów w tle
        String[] imagePaths = books.stream()
                .map(Book::getCoverImagePath)
                .filter(path -> path != null && !path.isEmpty())
                .toArray(String[]::new);

        if (imagePaths.length > 0) {
            client.preloadImages(imagePaths);
        }

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
        coverImageView.setSmooth(true);

        // nowe ladowanie
        loadBookCoverSmart(book, coverImageView);

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

        // Hover effects
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

    private void showSearchResultsAsync(List<Book> books) {
        // Wyczyść loading indicator
        searchBox.getChildren().clear();

        if (books.isEmpty()) {
            Label noResultsLabel = new Label("No books found. Try a different search term.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-padding: 20px; -fx-text-fill: #666;");
            searchBox.getChildren().add(noResultsLabel);
            return;
        }

        // Preload obrazów w tle
        String[] imagePaths = books.stream()
                .map(Book::getCoverImagePath)
                .filter(path -> path != null && !path.isEmpty())
                .toArray(String[]::new);

        if (imagePaths.length > 0) {
            client.preloadImages(imagePaths);
        }

        // Dodawaj wyniki jeden po drugim z małym opóźnieniem
        addSearchResultsProgressive(books, 0);
    }

    /**
     * Progresywne dodawanie wyników wyszukiwania
     */
    private void addSearchResultsProgressive(List<Book> books, int currentIndex) {
        if (currentIndex >= books.size()) {
            return; // Zakończ gdy wszystkie dodane
        }

        Book book = books.get(currentIndex);

        // Dodaj pojedynczy wynik
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/searchBokk.fxml"));
                HBox searchResult = loader.load();

                SearchController controller = loader.getController();

                // WAŻNE: Ustaw podstawowe dane synchronicznie
                controller.setBasicData(book, currentUser.getUsername());
                controller.setMainController(this);

                searchBox.getChildren().add(searchResult);

                // Załaduj dodatkowe dane asynchronicznie
                controller.loadAdditionalDataAsync();

            } catch (IOException e) {
                System.err.println("Error loading search result for book: " + book.getTitle());
                e.printStackTrace();
            }

            // Zaplanuj dodanie kolejnego wyniku po krótkim opóźnieniu
            if (currentIndex + 1 < books.size()) {
                Timeline timeline = new Timeline(new KeyFrame(
                        Duration.millis(50), // 50ms opóźnienia między elementami
                        e -> addSearchResultsProgressive(books, currentIndex + 1)
                ));
                timeline.play();
            }
        });
    }

    public void onSearchClicked() {
        hideUserMenu();
        hideNotificationMenu();

        String searchTerm = searchField.getText();
        if (searchTerm == null || searchTerm.isBlank()) return;

        Request request = new Request(RequestType.SEARCH_BOOK, searchTerm);

        client.executeAsyncWithData(request, new Client.ResponseHandler() {
            @Override
            public void handle(Response response) {
                if (response.getType() == ResponseType.SUCCESS) {
                    @SuppressWarnings("unchecked")
                    List<Book> results = (List<Book>) response.getData();

                    showSearchResults(results);
                    loadRandomQuote();
                    searchPane.setVisible(true);
                    homePane.setVisible(false);
                    bookDetailsPane.setVisible(false);
                    statisticsPane.setVisible(false);

                    System.out.println("Search completed for: " + searchTerm);
                } else {
                    System.err.println("Search failed: " + response.getData());
                }
            }

            @Override
            public void handleError(Exception e) {
                System.err.println("Search error: " + e.getMessage());
            }
        });
    }

    public void onHomeClicked() {
        hideUserMenu();
        hideNotificationMenu();
        clearSearchField();

        loadTopRatedBooks();
        loadRandomQuote();
        homePane.setVisible(true);
        searchPane.setVisible(false);
        bookDetailsPane.setVisible(false);
        statisticsPane.setVisible(false);
    }

    //BOOK DETAILS
    public void showBookDetails(Book book) {
        hideUserMenu();
        hideNotificationMenu();
        clearSearchField();

        if (book == null) return;

        loadRandomQuote();

        currentBookDetails = book;
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
            detailsReviews.setText(book.getReviewCount() + " reviews");
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
            String imagePath = book.getCoverImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                Image cachedImage = client.getImageFX(imagePath);
                if (cachedImage != null) {
                    coverBookDetails.setImage(cachedImage);
                    System.out.println("Details cover loaded from cache: " + imagePath);
                } else {
                    // Fallback do lokalnych zasobów
                    try {
                        String fullResourcePath = "/img/" + imagePath;
                        URL imageUrl = getClass().getResource(fullResourcePath);
                        if (imageUrl != null) {
                            Image coverImage = new Image(imageUrl.toString());
                            coverBookDetails.setImage(coverImage);
                        } else {
                            setDefaultDetailsCoverImage();
                        }
                    } catch (Exception e) {
                        setDefaultDetailsCoverImage();
                        System.err.println("Error loading book details cover: " + e.getMessage());
                    }
                }
            } else {
                setDefaultDetailsCoverImage();
            }

            homePane.setVisible(false);
            searchPane.setVisible(false);
            bookDetailsPane.setVisible(true);
            statisticsPane.setVisible(false);
        }


        updateStarRatingD(book.getAverageRating());
        loadBookReviews();

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

    /**
     * Ładuje reviews dla aktualnie wyświetlanej książki
     */
    private void loadBookReviews() {
        if (currentBookDetails == null) return;

        try {
            System.out.println("Proba pobrania recenzji");
            Request request = new Request(RequestType.GET_BOOK_REVIEWS, currentBookDetails.getBookId());
            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                @SuppressWarnings("unchecked")
                List<Review> reviews = (List<Review>) response.getData();
                List<Review> reviewsWithContent = reviews.stream()
                        .filter(review -> review.getReviewText() != null &&
                                !review.getReviewText().trim().isEmpty())
                        .collect(Collectors.toList());
                displayReviews(reviewsWithContent);
            } else {
                System.err.println("Error loading reviews: " + response.getData());
            }

        } catch (Exception e) {
            System.err.println("Exception loading reviews: " + e.getMessage());
        }
    }

    /**
     * Wyświetla reviews w kontenerze
     */
    private void displayReviews(List<Review> reviews) {
        if (reviewsContainer == null) return;

        reviewsContainer.getChildren().clear();
        System.out.println("Displaying reviews: " + reviews.size());

        for (Review review : reviews) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/reviewItem.fxml"));
                VBox reviewItem = loader.load();

                ReviewItemController controller = loader.getController();
                controller.setReviewData(review, currentUser.getUsername());

                reviewsContainer.getChildren().add(reviewItem);

            } catch (Exception e) {
                System.err.println("Error loading review item: " + e.getMessage());
            }
        }
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

        Map<String, Object> data = new HashMap<>();
        data.put("username", currentUser.getUsername());
        data.put("bookId", currentBookDetails.getBookId());

        Request request = new Request(RequestType.GET_READING_STATUS, data);

        client.executeAsyncWithData(request, new Client.ResponseHandler() {
            @Override
            public void handle(Response response) {
                isUpdatingStatus = true;
                try {
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
                } finally {
                    isUpdatingStatus = false;
                }
            }

            @Override
            public void handleError(Exception e) {
                System.err.println("Exception loading reading status: " + e.getMessage());
                isUpdatingStatus = true;
                detailsStatusCombo.getSelectionModel().select(WANT_TO_READ);
                setBookDetailsComboBoxStyle(true);
                isUpdatingStatus = false;
            }
        });
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
                    openReviewWindow(currentBookDetails, currentUser.getUsername());
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

 /**Inteligentne ładowanie okładki: cache -> serwer -> lokalne zasoby
 */
    private void loadBookCoverSmart(Book book, ImageView imageView) {
        String imagePath = book.getCoverImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            setDefaultCoverImage(imageView);
            return;
        }

        // Sprawdź czy obraz jest już w cache klienta
        Image cachedImage = client.getImageFX(imagePath);
        if (cachedImage != null) {
            imageView.setImage(cachedImage);
            System.out.println("✅ Loaded from CLIENT CACHE: " + imagePath);
            return;
        }

        // Uruchom w osobnym wątku żeby nie blokować UI
        new Thread(() -> {
            try {
                // Spróbuj pobrać z serwera (thumbnail)
                Image serverImage = client.getImageFX(imagePath);

                Platform.runLater(() -> {
                    if (serverImage != null) {
                        imageView.setImage(serverImage);
                        System.out.println("✅ Loaded from SERVER: " + imagePath);
                    } else {
                        // Fallback - spróbuj lokalnie
                        loadLocalImage(book, imageView);
                    }
                });

            } catch (Exception e) {
                System.err.println("Error loading from server: " + e.getMessage());
                Platform.runLater(() -> loadLocalImage(book, imageView));
            }
        }).start();
    }

    /**
     * Ładowanie z lokalnych zasobów (fallback)
     */
    private void loadLocalImage(Book book, ImageView imageView) {
        try {
            String imagePath = book.getCoverImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                String fullResourcePath = "/img/" + imagePath;
                URL imageUrl = getClass().getResource(fullResourcePath);
                if (imageUrl != null) {
                    Image coverImage = new Image(imageUrl.toString());
                    imageView.setImage(coverImage);
                    System.out.println("✅ Loaded LOCALLY: " + fullResourcePath);
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading local image: " + e.getMessage());
        }

        // Ostateczny fallback
        setDefaultCoverImage(imageView);
        System.out.println("🔄 Using DEFAULT image");
    }



    //EDIT OR ADD REVIEW FROM DETAILS
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
                // Odśwież rating status label
                loadBookDetailsUserRating();
                loadBookReviews();
            });

            reviewStage.show();

        } catch (Exception e) {
            System.err.println("Error opening review window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onEditRatingButtonClicked() {
        if (currentBookDetails != null && currentUser != null) {
            openReviewWindow(currentBookDetails, currentUser.getUsername());
        }
    }

    public void onStatisticsClicked() {
        hideUserMenu();
        hideNotificationMenu();
        clearSearchField();

        loadStatisticsPane();
        loadRandomQuote();
        statisticsPane.setVisible(true);
        homePane.setVisible(false);
        searchPane.setVisible(false);
        bookDetailsPane.setVisible(false);
    }

    private void loadStatisticsPane() {
        System.out.println("Loading statistics pane...");

        if (statisticsPane == null) {
            System.err.println("Statistics pane is null!");
            return;
        }

        System.out.println("Statistics pane found, attempting to load FXML...");

        try {
            java.net.URL fxmlUrl = getClass().getResource("/com/program/bookie/statistics.fxml");
            if (fxmlUrl == null) {
                System.err.println("FXML file not found: /com/program/bookie/statistics.fxml");
                return;
            }

            System.out.println("FXML file found at: " + fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent statisticsContent = loader.load();

            System.out.println("FXML loaded successfully");

            StatisticsController statisticsController = loader.getController();
            if (statisticsController == null) {
                System.err.println("Statistics controller is null!");
                return;
            }

            System.out.println("Statistics controller found");

            statisticsController.setCurrentUser(currentUser);

            statisticsPane.getChildren().clear();
            statisticsPane.getChildren().add(statisticsContent);

            System.out.println("Statistics content added to pane successfully");

        } catch (Exception e) {
            System.err.println("Error loading statistics pane: " + e.getMessage());
            e.printStackTrace();

            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label("Error loading statistics: " + e.getMessage());
            statisticsPane.getChildren().clear();
            statisticsPane.getChildren().add(errorLabel);
        }
    }

    public void onShelfClicked() {
        hideUserMenu();
        hideNotificationMenu();
        clearSearchField();

        System.out.println("Shelves clicked - funkcja do zaimplementowania w przyszłości");
    }

    //NOTIFICATIONS
    private void updateNotificationBadge(int count) {
        Platform.runLater(() -> {
            if (notificationBadge != null && countNLabel != null) {
                if (count > 0) {
                    notificationBadge.setVisible(true);
                    countNLabel.setText(count > 9 ? "9+" : String.valueOf(count));
                } else {
                    notificationBadge.setVisible(false);
                }
            }
        });
    }
    @FXML
    private void toggleNotificationMenu() {
        isNotificationMenuVisible = !isNotificationMenuVisible;

        if (notificationDropdown != null) {
            notificationDropdown.setVisible(isNotificationMenuVisible);

            if (isNotificationMenuVisible) {
                hideUserMenu();


                notificationService.loadNotifications(false);
                displayNotifications();


                Timeline timeline = new Timeline(new KeyFrame(
                        Duration.seconds(2),
                        e -> markVisibleNotificationsAsRead()
                ));
                timeline.play();
            }
        }
    }
    private void displayNotifications() {

        if (notificationsList == null) {
            System.err.println("notificationList is null!");
            return;
        }

        notificationsList.getChildren().clear();

        ObservableList<INotification> notifications = notificationService.getNotifications();

        if (notifications.isEmpty()) {
            Label emptyLabel = new Label("No notifications");
            emptyLabel.setStyle("-fx-padding: 20; -fx-text-fill: #888;");
            notificationsList.getChildren().add(emptyLabel);
        } else {
            for (int i = 0; i < notifications.size(); i++) {
                INotification notification = notifications.get(i);

                try {
                    VBox notificationItem = createNotificationItem(notification);
                    notificationsList.getChildren().add(notificationItem);
                    System.out.println("Added notification item " + i + " to list");
                } catch (Exception e) {
                    System.err.println("Error creating notification item " + i + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("📊 Final notificationsList children count: " + notificationsList.getChildren().size());
    }

    // Stworz powiadomienie
    private VBox createNotificationItem(INotification notification) {
        VBox item = new VBox(5);
        String backgroundColor = notification.isRead() ? "#f9f9f9" : "#fff";
        item.setStyle("-fx-padding: 10; -fx-background-color: " + backgroundColor +
                "; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Header with icon and time
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        String iconText = notification.getIcon();
        Label iconLabel = new Label(iconText);
        iconLabel.setStyle("-fx-font-size: 20;");

        String timeText = notification.getFormattedTime();
        Label timeLabel = new Label(timeText);
        timeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(iconLabel, spacer, timeLabel);

        // Title
        String titleText = notification.getTitle();
        Label titleLabel = new Label(titleText);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        // Message
        String messageText = notification.getMessage();
        Label messageLabel = new Label(messageText);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

        item.getChildren().addAll(header, titleLabel, messageLabel);

        // Click handler
        item.setOnMouseClicked(event -> {
            System.out.println("Notification clicked: " + notification.getTitle());
            notification.handleClick(this);
            hideNotificationMenu();
        });

        // Hover effect
        item.setOnMouseEntered(e -> {
            item.setStyle(item.getStyle() + "; -fx-background-color: #f0f0f0; -fx-cursor: hand;");
        });
        item.setOnMouseExited(e -> {
            item.setStyle(item.getStyle().replace("; -fx-background-color: #f0f0f0; -fx-cursor: hand;", ""));
        });

        return item;
    }

    public void openBookFromNotification(int bookId) {
        System.out.println("Opening book from notification, book ID: " + bookId);

        new Thread(() -> {
            try {
                Request request = new Request(RequestType.GET_BOOK_BY_ID, bookId);
                Response response = client.sendRequest(request);

                if (response.getType() == ResponseType.SUCCESS) {
                    Book book = (Book) response.getData();

                    Platform.runLater(() -> {
                        System.out.println("Successfully loaded book: " + book.getTitle());
                        showBookDetails(book);
                    });

                } else {
                    System.err.println(" Error loading book: " + response.getData());
                    Platform.runLater(() -> {
                        System.err.println("Failed to load book details");
                    });
                }

            } catch (Exception e) {
                System.err.println("Exception loading book: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }


    private void markVisibleNotificationsAsRead() {
        ObservableList<INotification> notifications = notificationService.getNotifications();
        List<Integer> unreadIds = new ArrayList<>();

        for (INotification notif : notifications) {
            if (!notif.isRead()) {
                unreadIds.add(notif.getNotificationId());
            }
        }

        if (!unreadIds.isEmpty()) {
            notificationService.markAsRead(unreadIds);
        }
    }


    private void hideNotificationMenu() {
        isNotificationMenuVisible = false;
        if (notificationDropdown != null) {
            notificationDropdown.setVisible(false);
        }
    }

    private void setupNotificationBindings() {
        if (notificationService != null && notificationBadge != null && countNLabel != null) {
            // Bind badge visibility to unread count
            notificationBadge.visibleProperty().bind(
                    Bindings.greaterThan(notificationService.unreadCountProperty(), 0)
            );

            // Bind label text to unread count
            countNLabel.textProperty().bind(
                    Bindings.createStringBinding(() -> {
                        int count = notificationService.getUnreadCount();
                        return count > 9 ? "9+" : String.valueOf(count);
                    }, notificationService.unreadCountProperty())
            );

            System.out.println("Notification bindings established");
        }
    }

}