package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("app.fxml"));
        Scene scene = new Scene(loader.load());

        AppController appController = loader.getController();
        appController.setPrimaryStage(stage);
        //appController.injectStage();

        stage.setScene(scene);
        stage.setTitle("S-EMULATOR");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
