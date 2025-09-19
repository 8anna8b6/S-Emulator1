package app.header;

import app.programTable.ProgramTableController;
import execute.EngineImpl;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
    @FXML private MenuButton themeMenuButton;
    @FXML private ComboBox<String> highlightComboBox;

    private Consumer<String> onFileLoaded;
    private ProgramTableController programTabController;

    // Light mode
    @FXML
    private void setLightMode() {
        Stage stage = (Stage) themeMenuButton.getScene().getWindow();
        stage.getScene().getStylesheets().clear();
        stage.getScene().getStylesheets().add(
                getClass().getResource("/app/resources/styles/light.css").toExternalForm()
        );
    }

    // Dark mode
    @FXML
    private void setDarkMode() {
        Stage stage = (Stage) themeMenuButton.getScene().getWindow();
        stage.getScene().getStylesheets().clear();
        stage.getScene().getStylesheets().add(
                getClass().getResource("/app/resources/styles/dark.css").toExternalForm()
        );
    }

    @FXML
    private void loadFileButtonAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );

        File selectedFile = fileChooser.showOpenDialog(themeMenuButton.getScene().getWindow());

        if (selectedFile != null) {
            simulateFileLoading(selectedFile);
        } else {
            filePathLabel.setText("No file selected");
        }
    }

    private void simulateFileLoading(File file) {
        Task<Boolean> loadTask = new Task<>() {
            @Override
            protected Boolean call() {
                int steps = 50;
                for (int i = 1; i <= steps; i++) {
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException ignored) {}
                    updateProgress(i, steps);
                }

                EngineImpl engine = new EngineImpl(); // Or use shared instance
                return engine.loadFromXML(file.getAbsolutePath());
            }
        };

        progressBar.progressProperty().bind(loadTask.progressProperty());

        loadTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);

            boolean success = loadTask.getValue();
            if (success) {
                filePathLabel.setText(file.getAbsolutePath());
                if (onFileLoaded != null) {
                    onFileLoaded.accept(file.getAbsolutePath());
                }
            } else {
                showAlert(Alert.AlertType.ERROR,
                        "File Load Error",
                        "The selected file could not be loaded.\n" +
                                "Please check that it is a valid XML program file.");
                filePathLabel.setText("Error: failed to load file");
            }
        });

        loadTask.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);

            Throwable ex = loadTask.getException();
            showAlert(Alert.AlertType.ERROR,
                    "Unexpected Error",
                    "An unexpected error occurred while loading the file:\n" +
                            (ex != null ? ex.getMessage() : "Unknown error"));
            filePathLabel.setText("Error: failed to load file");
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }



    @FXML
    private void expandProgramAction() {
        if (programTabController == null) return;

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

    public void setOnFileLoaded(Consumer<String> callback) {
        this.onFileLoaded = callback;
    }

    public void setProgramTabController(ProgramTableController controller) {
        this.programTabController = controller;

        if (programTabController == null || highlightComboBox == null) return;

        // backing observable list from ProgramTableController (this is updated there)
        ObservableList<String> backing = programTabController.getVariableNames();

        // comboItems = one empty option + backing content (kept in sync below)
        ObservableList<String> comboItems = FXCollections.observableArrayList();
        comboItems.add("");               // empty first entry = "no highlight"
        comboItems.addAll(backing);       // initial snapshot

        // use comboItems as the ComboBox items
        highlightComboBox.setItems(comboItems);

        // show empty as default (no highlight)
        highlightComboBox.getSelectionModel().selectFirst();

        // Keep comboItems in sync whenever backing changes
        backing.addListener((ListChangeListener<String>) change -> {
            Platform.runLater(() -> {
                String currentSelection = highlightComboBox.getValue();

                comboItems.clear();
                comboItems.add("");
                comboItems.addAll(backing);

                // restore previous selection if still present, otherwise select empty
                if (currentSelection != null && comboItems.contains(currentSelection)) {
                    highlightComboBox.getSelectionModel().select(currentSelection);
                } else {
                    highlightComboBox.getSelectionModel().selectFirst();
                }
            });
        });

        // Handle user selection: empty = clear highlight
        highlightComboBox.setOnAction(e -> {
            String selected = highlightComboBox.getValue();
            if (selected != null && !selected.isEmpty()) {
                programTabController.highlightVariable(selected);
            } else {
                programTabController.highlightVariable(null);
            }
        });
    }
}
