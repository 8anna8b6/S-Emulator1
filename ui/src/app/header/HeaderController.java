package app.header;

import app.programTable.ProgramTableController;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;

public class HeaderController {

    @FXML private Label filePathLabel;
    @FXML private ProgressBar progressBar;


    private Stage primaryStage; //reference to the main application window
    private Consumer<String> onFileLoaded;
    private ProgramTableController programTabController;

    private boolean darkMode = false;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setOnFileLoaded(Consumer<String> callback) {
        this.onFileLoaded = callback;
    }

    public void setProgramTabController(ProgramTableController controller) {
        this.programTabController = controller;
    }



    @FXML
    private void setLightMode() {
        if (primaryStage == null) return;
        primaryStage.getScene().getStylesheets().clear();
        primaryStage.getScene().getStylesheets().add(
                getClass().getResource("../resources/styles/light.css").toExternalForm()
        );
    }

    @FXML
    private void setDarkMode() {
        if (primaryStage == null) return;
        primaryStage.getScene().getStylesheets().clear();
        primaryStage.getScene().getStylesheets().add(
                getClass().getResource("../resources/styles/dark.css").toExternalForm()
        );
    }

    @FXML
    private void loadFileButtonAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );

        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            simulateFileLoading(selectedFile);
        } else {
            filePathLabel.setText("No file selected");
        }
    }


    private void simulateFileLoading(File file) {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int steps = 50;
                for (int i = 1; i <= steps; i++) {
                    Thread.sleep(30);
                    updateProgress(i, steps);
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(loadTask.progressProperty());

        loadTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            filePathLabel.setText(file.getAbsolutePath());

            if (onFileLoaded != null) {
                onFileLoaded.accept(file.getAbsolutePath());
            }
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }


    @FXML
    private void runProgramAction() {
        if (programTabController != null) {
            programTabController.runProgramAction();
        }
    }

    @FXML
    private void expandProgramAction() {
        if (programTabController == null) return;

        // Ask for degree
        int maxDegree = programTabController.getMaxDegree();

        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Expand Program");
        dialog.setHeaderText("Choose Expansion Degree");
        dialog.setContentText("Enter degree (0 - " + maxDegree + "):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                int degree = Integer.parseInt(input);

                if (degree < 0 || degree > maxDegree) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Degree",
                            "Please enter a value between 0 and " + maxDegree);
                    return;
                }

                // Call expandProgram on controller
                programTabController.expandProgram(degree);

            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input",
                        "Please enter a valid integer.");
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
