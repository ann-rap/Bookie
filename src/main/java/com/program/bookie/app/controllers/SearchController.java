package com.program.bookie.app.controllers;

import com.program.bookie.models.*;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.program.bookie.models.ResponseType.*;
import com.program.bookie.client.Client;
import com.program.bookie.models.ResponseType.*;


import java.util.HashMap;
import java.util.Map;


public class SearchController {

    private Client client = Client.getInstance();

    @FXML
    private ImageView bookCover;
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

    private final String[] statuses = {"Want to read", "Currently reading", "Read"};
    private final String WANT_TO_READ = "Want to read";

    private String currentUsername;
    private int currentBookId;

    public void setData(Book book, String username) {
        this.currentUsername = username;
        this.currentBookId = book.getBookId();

        titleLabel.setText(book.getTitle());
        authorLabel.setText("by "+ book.getAuthor());
        avgRatingLabel.setText(String.format("★%.1f", book.getAverageRating())+" avg rating");
        ratingCountLabel.setText(String.valueOf(book.getRatingCount())+ " ratings");
        publicationYearLabel.setText("published in "+ String.valueOf(book.getPublicationYear()));

        if (book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
            Image image = new Image(getClass().getResourceAsStream("/img/" + book.getCoverImagePath()));
            bookCover.setImage(image);


            double fitWidth = 150;
            double fitHeight = 200;
            bookCover.setFitWidth(fitWidth);
            bookCover.setFitHeight(fitHeight);
            bookCover.setPreserveRatio(true);
            bookCover.setSmooth(true);

            //Dopasowanie wymiarow okladki
            double imageWidth = image.getWidth();
            double imageHeight = image.getHeight();

            double scaleX = imageWidth / fitWidth;
            double scaleY = imageHeight / fitHeight;


            double scale = Math.min(scaleX, scaleY);

            double viewportWidth = fitWidth * scale;
            double viewportHeight = fitHeight * scale;


            double viewportX = (imageWidth - viewportWidth) / 2;
            double viewportY = (imageHeight - viewportHeight) / 2;

            // Ustaw viewport
            bookCover.setViewport(new Rectangle2D(viewportX, viewportY, viewportWidth, viewportHeight));
        }

        //COMBOBOX
        statusComboBox.getItems().setAll(statuses);

        loadReadingStatus(currentUsername,currentBookId);

    }

    private void setComboBoxCellFactory(boolean firstOptionGreen) {
        statusComboBox.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (firstOptionGreen && item.equals(WANT_TO_READ)) {
                        setStyle("-fx-background-color: #658C4C;");
                    } else {
                        setStyle("-fx-background-color: #d3d3d3;");
                    }
                }
            }
        });

        // Ustawienie wyglądu zaznaczonej (wyświetlanej) wartości w ComboBox
        statusComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (firstOptionGreen && item.equals(WANT_TO_READ)) {
                        setStyle("-fx-background-color: #658C4C;");
                    } else {
                        setStyle("-fx-background-color: #d3d3d3;");
                    }
                }
            }
        });
    }

    public void loadReadingStatus(String username, int bookId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("bookId", bookId);

            Request request = new Request(RequestType.GET_READING_STATUS, data);
            Response response = client.sendRequest(request);

            statusComboBox.getItems().setAll(statuses);

            if (response.getType() == ResponseType.SUCCESS) {
                String status = (String) response.getData();
                if (status == null) {
                    statusComboBox.getSelectionModel().select(WANT_TO_READ);
                    setComboBoxCellFactory(true);
                } else {
                    statusComboBox.getSelectionModel().select(status);
                    setComboBoxCellFactory(false);
                }
            } else {
                System.err.println("Błąd pobierania statusu: " + response.getData());
                statusComboBox.getSelectionModel().select(WANT_TO_READ);
                setComboBoxCellFactory(true);
            }

            statusComboBox.setOnAction(e -> {
                String selectedStatus = statusComboBox.getSelectionModel().getSelectedItem();
                try {
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("username", username);
                    updateData.put("bookId", bookId);
                    updateData.put("status", selectedStatus);

                    Request updateRequest = new Request(RequestType.UPDATE_READING_STATUS, updateData);
                    Response updateResponse = client.sendRequest(updateRequest);

                    if (updateResponse.getType() == ResponseType.SUCCESS) {
                        setComboBoxCellFactory(false);
                    } else {
                        System.err.println("Błąd aktualizacji statusu: " + updateResponse.getData());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}