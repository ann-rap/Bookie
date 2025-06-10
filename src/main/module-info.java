module com.program.bookie {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires javafx.media;
    requires com.mysql.cj;

    opens com.program.bookie to javafx.fxml;
    opens com.program.bookie.app.controllers to javafx.fxml;
    opens com.program.bookie.db to javafx.fxml;
}