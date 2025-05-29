package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.stage.StageStyle;


import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private Button cancelButton;
    @FXML
    private Label loginMessageLabel;
    @FXML
    private TextField usernameTextField;
    @FXML
    private PasswordField enterPasswordField;
    @FXML
    private Button registerButton;


    private Client client;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        client = Client.getInstance();

        if (!client.connect()) {
            loginMessageLabel.setText("Cannot connect to server");
        }
    }

    public void loginButtonOnAction(ActionEvent event) {
        if(!usernameTextField.getText().isEmpty() && !enterPasswordField.getText().isEmpty()) {
            validateLogin();
        } else {
            loginMessageLabel.setText("Please enter your username and password");
        }
    }

    public void cancelButtonOnAction(ActionEvent event) {
        if (client != null) {
            System.out.println("Disconnecting client...");
            client.disconnect();
        }
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void validateLogin() {
        String username = usernameTextField.getText().trim();
        String password = enterPasswordField.getText();

        try {
            LoginData loginData = new LoginData(username, password);
            Request request = new Request(RequestType.LOGIN, loginData);

            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                User user = (User) response.getData();
                openMainWindow(user);


            } else {
                loginMessageLabel.setText((String) response.getData());
            }

        } catch (Exception e) {
            loginMessageLabel.setText("Connection error: " + e.getMessage());
        }
    }

    public void openMainWindow(User u){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/mainWindow.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setCurrentUser(u);
            controller.loadTopRatedBooks();


            Stage registerStage = new Stage();
            registerStage.setTitle("Bookie");
            registerStage.initStyle(StageStyle.UNDECORATED);
            registerStage.setScene(new Scene(root, 1000, 600));
            registerStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icon.png"))));

            registerStage.setOnCloseRequest(event -> {
                if (client != null) {
                    client.disconnect();
                }
            });

            Stage currentStage = (Stage) registerButton.getScene().getWindow();
            currentStage.close();
            registerStage.show();

        } catch (Exception e) {
            System.err.println("Error loading home page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void createAccountForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/program/bookie/register.fxml"));
            Parent root = loader.load();



            Stage registerStage = new Stage();
            registerStage.setTitle("Register");
            registerStage.initStyle(StageStyle.UNDECORATED);
            registerStage.setScene(new Scene(root, 520, 476));
            registerStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icon.png"))));

            registerStage.setOnCloseRequest(event -> {
                if (client != null) {
                    client.disconnect();
                }
            });

            Stage currentStage = (Stage) registerButton.getScene().getWindow();
            currentStage.close();
            registerStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}