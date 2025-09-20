package app;

import app.header.HeaderController;
import app.programTable.ProgramTableController;
import app.instrHistory.InstrHistoryController;
import app.historyTable.HistoryTableController;
import app.runMenu.RunMenuController;
import execute.EngineImpl;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppController {

    // Top header
    @FXML private HeaderController headerPaneController;

    // Center
    @FXML private ProgramTableController programTablePaneController;
    @FXML private RunMenuController runMenuController;

    // Bottom
    @FXML private InstrHistoryController instrHistoryPaneController;
    @FXML private HistoryTableController historyTablePaneController;

    private Stage primaryStage;
    private final EngineImpl engine = new EngineImpl();
    private Scene scene;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    /** Apply light mode */
    public void setLightMode() {
        if (scene != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                    getClass().getResource("resources/styles/light.css").toExternalForm()
            );
        }
    }

    /** Apply dark mode */
    public void setDarkMode() {
        if (scene != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                    getClass().getResource("resources/styles/dark.css").toExternalForm()
            );
        }
    }

    @FXML
    public void initialize() {
        // Inject engine into controllers
        if (programTablePaneController != null) {
            programTablePaneController.setEngine(engine);
            programTablePaneController.setInstrHistoryController(instrHistoryPaneController);
            programTablePaneController.setHistoryTableController(historyTablePaneController);
        }

        if (runMenuController != null) {
            runMenuController.setEngine(engine);
            runMenuController.setHistoryController(historyTablePaneController);

            runMenuController.setProgramTableController(programTablePaneController);
        }

        if (headerPaneController != null) {
            headerPaneController.setProgramTabController(programTablePaneController);

            headerPaneController.setOnFileLoaded(filePath -> {
                if (programTablePaneController != null) {
                    programTablePaneController.loadProgram(filePath);
                }
                if (runMenuController != null) {
                    runMenuController.loadInputVariables();
                }
            });
        }
    }

    // Method to handle degree changes (if you have degree selection functionality)
    public void handleDegreeChange(int newDegree) {
        if (runMenuController != null) {
            runMenuController.setCurrentDegree(newDegree);
        }
        if (programTablePaneController != null) {
            programTablePaneController.expandProgram(newDegree);
        }
    }
}