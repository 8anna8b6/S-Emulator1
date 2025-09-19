package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("app.fxml"));
        Scene scene = new Scene(loader.load());

        AppController appController = loader.getController();
        appController.setPrimaryStage(stage);
        appController.setScene(scene);


        scene.getStylesheets().add(
                getClass().getResource("resources/styles/light.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("S-EMULATOR");

        // Set application icon
        Image icon = new Image(getClass().getResourceAsStream("resources/icon.png"));
        stage.getIcons().add(icon);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

