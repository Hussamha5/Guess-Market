package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // טעינת קובץ העיצוב שנייצר מיד
        Parent root = FXMLLoader.load(getClass().getResource("/MainWindow.fxml"));

        primaryStage.setTitle("Guess Market - JavaFX");
        primaryStage.setScene(new Scene(root, 800, 600)); // גודל החלון
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}