package com.program.bookie.app.controllers;

import com.program.bookie.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;


public class ReviewController {

    @FXML
    private ImageView star1,star2,star3,star4,star5;

    @FXML
    private Button closeButton;

    private int review=0;

    public void closeButtonOnAction(ActionEvent event) {
        /*if (client != null) {
            client.disconnect();
        }*/
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    public void star1OnAction(ActionEvent event) {
        review=1;
        starOnAction(star1,true);
        starOnAction(star2,false);
        starOnAction(star3,false);
        starOnAction(star4,false);
        starOnAction(star5,false);
    }
    public void star2OnAction(ActionEvent event) {
        review=2;
        starOnAction(star1,true);
        starOnAction(star2,true);
        starOnAction(star3,false);
        starOnAction(star4,false);
        starOnAction(star5,false);
    }
    public void star3OnAction(ActionEvent event) {
        review=3;
        starOnAction(star1,true);
        starOnAction(star2,true);
        starOnAction(star3,true);
        starOnAction(star4,false);
        starOnAction(star5,false);
    }
    public void star4OnAction(ActionEvent event) {
        review=4;
        starOnAction(star1,true);
        starOnAction(star2,true);
        starOnAction(star3,true);
        starOnAction(star4,true);
        starOnAction(star5,false);
    }
    public void star5OnAction(ActionEvent event) {
        review=5;
        starOnAction(star1,true);
        starOnAction(star2,true);
        starOnAction(star3,true);
        starOnAction(star4,true);
        starOnAction(star5,true);
    }

    public void starOnAction(ImageView star, boolean selected)
    {
        String imagePath = selected ? "/img/star.png" : "/img/star2.png";;
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
        star.setImage(image);
    }


}
