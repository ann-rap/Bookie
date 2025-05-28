package com.program.bookie.app.controllers;

import com.program.bookie.client.Client;
import com.program.bookie.models.*;  // assuming your models package contains Request, Response, etc.
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private Button goBackButton;
    @FXML private TextField firstnameTextField;
    @FXML private TextField lastnameTextField;
    @FXML private TextField usernameTextField;
    @FXML private PasswordField setPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label confirmLabel;
    @FXML private Label informationLabel;

    private Client client;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        client = Client.getInstance();

        if (!client.connect()) {
            informationLabel.setText("Cannot connect to server");
        }
    }

    public void registerButtonAction(ActionEvent event) {
        informationLabel.setText("");
        confirmLabel.setText("");

        String firstname = firstnameTextField.getText().trim();
        String lastname = lastnameTextField.getText().trim();
        String username = usernameTextField.getText().trim();
        String password = setPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (firstname.isEmpty() || lastname.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            informationLabel.setText("Please fill in all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmLabel.setText("Passwords don't match.");
            return;
        }

        if (!client.connect()) {
            informationLabel.setText("Unable to connect to server.");
            return;
        }

        registerUser(firstname, lastname, username, password);
    }

    private void registerUser(String firstname, String lastname, String username, String password) {
        try {
            RegisterData registerData = new RegisterData(firstname, lastname, username, password);
            Request request = new Request(RequestType.REGISTER, registerData);

            Response response = client.sendRequest(request);

            if (response.getType() == ResponseType.SUCCESS) {
                informationLabel.setStyle("-fx-text-fill: #1c3e78;");
                informationLabel.setText("Registration successful!");

                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(e -> {
                    client.disconnect();  // disconnect after success
                    goBackButtonAction();
                });
                pause.play();

            } else {
                informationLabel.setStyle("-fx-text-fill: red;");
                informationLabel.setText((String) response.getData());
                client.disconnect();
            }

        } catch (Exception e) {
            informationLabel.setStyle("-fx-text-fill: red;");
            informationLabel.setText("Connection error: " + e.getMessage());
            e.printStackTrace();
            client.disconnect();
        }
    }

    public void goBackButtonAction() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/program/bookie/login.fxml")));
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.setScene(new Scene(root, 520, 400));
            loginStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icon.png"))));
            Stage stage = (Stage) goBackButton.getScene().getWindow();
            stage.close();
            loginStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
