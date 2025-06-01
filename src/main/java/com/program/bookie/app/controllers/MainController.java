package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.StageStyle;
import javafx.application.Platform;
import java.util.Objects;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    //MENU
    @FXML
    private Button closeButton,miniButton,homeButton,shelfButton,statisticsButton,searchButton;
    //HOMEPAGE
    @FXML
    private Label welcomeLabel;

    @FXML
    private HBox booksContainer;

    @FXML
    private VBox searchBox;

    @FXML
    private Pane homePane, searchPane, bookDetailsPane;

    @FXML
    private Label detailsTitle, detailsAuthor, detailsRatings,detailsReviews,detailsDescription, detailsAvgRating;

    @FXML
    private TextField searchField;

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

    private boolean isUserMenuVisible = false;

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

        Platform.runLater(() -> {
            if (userButton.getScene() != null) {
                userButton.getScene().setOnMouseClicked(event -> {
                    if (!isClickOnUserMenu(event.getTarget()) && isUserMenuVisible) {
                        hideUserMenu();
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
            // TODO: Otwórz szczegóły książki
            System.out.println("Clicked on book: " + book.getTitle());
        });

        return bookCard;
    }

    private void setDefaultCoverImage(ImageView imageView) {
        // Możesz ustawić domyślny obraz lub pozostawić puste
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

                showSearchResults(results); // populates the VBox
                searchPane.setVisible(true);
                homePane.setVisible(false); // switch view
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

    }


    public void toggleUserMenu(ActionEvent event) {
        isUserMenuVisible = !isUserMenuVisible;
        userDropdown.setVisible(isUserMenuVisible);

        if (isUserMenuVisible && currentUser != null) {
            userGreeting.setText("Hi " + currentUser.getUsername() + "!");
        }
    }

    public void onAccountSettingsClicked(ActionEvent event) {
        System.out.println("Account settings clicked - funkcja do zaimplementowania w przyszłości");
        hideUserMenu();
    }

    public void onLogoutClicked(ActionEvent event) {
        try {
            if (client != null) {
                System.out.println("Logging out user: " + currentUser.getUsername());
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

    private void hideUserMenu() {
        isUserMenuVisible = false;
        userDropdown.setVisible(false);
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

}


