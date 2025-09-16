package app;

import app.header.HeaderController;
import app.programTable.ProgramTableController;
import app.instrHistory.InstrHistoryController;
import app.historyTable.HistoryTableController;
import execute.EngineImpl;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class AppController {

    // Top header
    @FXML private HeaderController headerPaneController;

    // Center
    @FXML private ProgramTableController programTablePaneController;
    @FXML private app.runMenu.RunMenuController runMenuController;

    // Bottom
    @FXML private InstrHistoryController instrHistoryPaneController;
    @FXML private HistoryTableController historyTablePaneController;

    private Stage primaryStage;
    private final EngineImpl engine = new EngineImpl();

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);
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
            runMenuController.setHistoryController(historyTablePaneController); // inject history controller
        }

        if (headerPaneController != null) {
            headerPaneController.setPrimaryStage(primaryStage);
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

}

