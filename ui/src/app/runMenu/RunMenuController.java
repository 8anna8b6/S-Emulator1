package app.runMenu;

import execute.EngineImpl;
import execute.components.RunRecord;
import execute.dto.VariableDTO;
import app.historyTable.HistoryTableController;
import app.programTable.ProgramTableController;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.*;

public class RunMenuController {

    @FXML
    private void handleNewRun() {
        // Clear all previous execution data
        resetToInitialState();

        console.clear();
        console.appendText("=== NEW RUN STARTED ===\n");
        console.appendText("All previous execution data cleared.\n");
        console.appendText("Please set input values and choose execution mode.\n\n");

        // Reset engine state if needed
        if (engine != null) {
            engine.debugStop(); // Stop any ongoing debug session
            // Clear any stored execution state in engine
            engine.resetVars();
        }

        // Reload input variables to reset to default values
        loadInputVariables();

        console.appendText("Ready for new execution:\n");
        console.appendText("- Set input variable values in the table above\n");
        console.appendText("- Click 'Run' for normal execution\n");
        console.appendText("- Click 'Debug' to step through program line by line\n");
    }

    @FXML public VBox vBox;
    @FXML private TableView<VariableDTO> inputsTable;
    @FXML private TableColumn<VariableDTO, String> varColumn;
    @FXML private TableColumn<VariableDTO, Long> valueColumn;
    @FXML private ListView<String> resultsList;
    @FXML private TextArea console;
    @FXML private Button runButton;
    @FXML private Button debugButton;
    @FXML private Button stepOverButton;
    @FXML private Button resumeButton;
    @FXML private Button stopButton;
    @FXML private Button newRunButton;
    @FXML private Label cyclesLabel;

    // Controllers
    private HistoryTableController historyController;
    private ProgramTableController programTableController;
    private EngineImpl engine;

    // Data
    private final ObservableList<VariableDTO> inputVars = FXCollections.observableArrayList();
    private final Map<String, Long> editedValues = new HashMap<>();
    private final Map<String, Long> previousVariableValues = new HashMap<>();

    // State properties
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty debugging = new SimpleBooleanProperty(false);
    private final BooleanProperty debugPaused = new SimpleBooleanProperty(false);
    private final IntegerProperty debugLine = new SimpleIntegerProperty(0);
    private final IntegerProperty currentCycles = new SimpleIntegerProperty(0);

    private int currentDegree = 0;

    @FXML
    public void initialize() {
        setupInputTable();
        setupButtons();
        setupBindings();
        setupResultsList();
        resetToInitialState();
    }

    private void setupInputTable() {
        inputsTable.setItems(inputVars);
        inputsTable.setEditable(true);
        inputsTable.getColumns().setAll(varColumn, valueColumn);

        varColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue() == null ? "" : cd.getValue().getName()));
        varColumn.setStyle("-fx-alignment: CENTER;");

        valueColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                cd.getValue() == null ? 0L : cd.getValue().getValue()));

        // Make value column editable
        valueColumn.setCellFactory(col -> new TableCell<VariableDTO, Long>() {
            private final TextField tf = new TextField();

            {
                tf.setTextFormatter(new TextFormatter<String>(change ->
                        change.getControlNewText().matches("\\d*") ? change : null));
                tf.setOnAction(e -> commit());
                tf.focusedProperty().addListener((obs, was, is) -> { if (!is) commit(); });
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                tf.setPrefWidth(Double.MAX_VALUE);
            }

            private void commit() {
                VariableDTO row = getTableRow() == null ? null : getTableRow().getItem();
                if (row == null) return;
                String s = tf.getText();
                if (s == null || s.isEmpty()) {
                    editedValues.remove(row.getName());
                    return;
                }
                editedValues.put(row.getName(), Long.parseLong(s));
            }

            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                VariableDTO row = getTableRow() == null ? null : getTableRow().getItem();
                Long shown = (row != null && editedValues.containsKey(row.getName()))
                        ? editedValues.get(row.getName()) : item;
                tf.setText(shown == null ? "" : String.valueOf(shown));
                setGraphic(tf);
            }
        });
    }

    private void setupButtons() {
        newRunButton.setOnAction(event -> handleNewRun());
        runButton.setOnAction(event -> handleRun());
        debugButton.setOnAction(event -> handleDebug());
        stepOverButton.setOnAction(event -> handleStepOver());
        resumeButton.setOnAction(event -> handleResume());
        stopButton.setOnAction(event -> handleStop());
    }

    private void setupBindings() {
        // Button state bindings based on debug flow
        runButton.disableProperty().bind(
                debugging.or(running)
        );

        debugButton.disableProperty().bind(
                debugging.or(running)
        );

        stepOverButton.disableProperty().bind(
                debugging.not().or(debugPaused.not())
        );

        resumeButton.disableProperty().bind(
                debugging.not().or(debugPaused.not())
        );

        stopButton.disableProperty().bind(
                debugging.not()
        );

        // Input table should be editable only when not running/debugging
        inputsTable.disableProperty().bind(running.or(debugging));

        // Cycles display
        if (cyclesLabel != null) {
            cyclesLabel.textProperty().bind(
                    currentCycles.asString().concat(" cycles")
            );
        }
    }

    private void setupResultsList() {
        resultsList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);

                    // Highlight changed variables during debugging
                    if (debugging.get() && item.contains("=")) {
                        String varName = item.split("=")[0].trim();
                        if (isVariableChanged(varName)) {
                            setStyle("-fx-background-color: #ffeb3b; -fx-text-fill: #000000; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void resetToInitialState() {
        editedValues.clear();
        previousVariableValues.clear();
        resultsList.getItems().clear();
        currentCycles.set(0);
        running.set(false);
        debugging.set(false);
        debugPaused.set(false);
        console.clear();

        if (programTableController != null) {
            programTableController.clearDebugHighlight();
        }
    }

    @FXML
    private void handleRun() {
        if (engine == null || !engine.isLoaded()) {
            console.appendText("No program loaded.\n");
            return;
        }

        running.set(true);
        console.clear();
        console.appendText("Running program in normal mode...\n");

        try {
            List<VariableDTO> inputsToLoad = prepareInputs();
            engine.loadInputs(inputsToLoad);

            // Run the program
            long result = engine.runProgramAndRecord(currentDegree,
                    inputsToLoad.stream().map(VariableDTO::getValue).toList());

            // Update displays
            updateResultsDisplay();

            // Get final cycles
            List<RunRecord> history = engine.getHistory();
            if (!history.isEmpty()) {
                RunRecord lastRun = history.get(history.size() - 1);
                currentCycles.set(lastRun.getCycles());

                console.appendText("Program completed successfully!\n");
                console.appendText("Result: " + result + "\n");
                console.appendText("Total cycles: " + lastRun.getCycles() + "\n");
            }

            // Update history table
            if (historyController != null) {
                historyController.showHistory(engine.getHistory());
            }

        } catch (Exception e) {
            console.appendText("Execution error: " + e.getMessage() + "\n");
        }

        running.set(false);
    }

    @FXML
    private void handleDebug() {
        if (engine == null || !engine.isLoaded()) {
            console.appendText("No program loaded.\n");
            return;
        }

        debugging.set(true);
        debugPaused.set(true);
        debugLine.set(0);
        currentCycles.set(0);

        console.clear();
        console.appendText("Entering DEBUG mode...\n");
        console.appendText("Use 'Step Over' to execute instructions one by one.\n");
        console.appendText("Use 'Resume' to continue normal execution.\n");
        console.appendText("Use 'Stop' to halt execution.\n\n");

        try {
            // Prepare inputs and start debugging
            List<VariableDTO> inputsToLoad = prepareInputs();
            engine.loadInputs(inputsToLoad);
            engine.debugStart(currentDegree, inputsToLoad);

            // Store initial variable state
            storeCurrentVariableState();

            // Update initial display
            updateResultsDisplay();

            console.appendText("Debug mode ready. Program is paused at the beginning.\n");

        } catch (Exception e) {
            console.appendText("Error starting debug mode: " + e.getMessage() + "\n");
            debugging.set(false);
            debugPaused.set(false);
        }
    }

    @FXML
    private void handleStepOver() {
        if (!debugging.get() || !debugPaused.get()) return;

        try {
            // Store previous state for comparison
            storeCurrentVariableState();

            // Execute one step
            boolean continueDebugging = engine.debugStep(currentDegree);

            // Update debug line
            debugLine.set(engine.getDebugLine());
            currentCycles.set(engine.getCurrentCycles());

            // Update program table highlighting
            if (programTableController != null) {
                if (continueDebugging) {
                    programTableController.setDebugCurrentLine(debugLine.get());
                } else {
                    programTableController.clearDebugHighlight();
                }
            }

            // Update variable display
            updateResultsDisplay();

            console.clear();
            console.appendText("DEBUG: Executed line " + debugLine.get() + "\n");
            console.appendText("Current cycles: " + currentCycles.get() + "\n");

            if (!continueDebugging) {
                console.appendText("Program execution completed.\n");
                finishDebugging();
            } else {
                console.appendText("Program paused. Use 'Step Over' to continue or 'Resume' for normal execution.\n");
            }

        } catch (Exception e) {
            console.appendText("Debug step error: " + e.getMessage() + "\n");
            handleStop();
        }
    }

    @FXML
    private void handleResume() {
        if (!debugging.get() || !debugPaused.get()) return;

        debugPaused.set(false);
        console.clear();
        console.appendText("Resuming normal execution from debug mode...\n");

        if (programTableController != null) {
            programTableController.clearDebugHighlight();
        }

        try {
            // Continue execution without stepping
            while (engine.isDebugging()) {
                boolean continueDebugging = engine.debugStep(currentDegree);
                if (!continueDebugging) break;
            }

            // Update final state
            currentCycles.set(engine.getCurrentCycles());
            updateResultsDisplay();

            console.appendText("Program completed successfully!\n");
            console.appendText("Total cycles: " + currentCycles.get() + "\n");

            finishDebugging();

        } catch (Exception e) {
            console.appendText("Resume error: " + e.getMessage() + "\n");
            handleStop();
        }
    }

    @FXML
    private void handleStop() {
        console.appendText("Execution stopped by user.\n");

        if (engine != null) {
            engine.debugStop();
        }

        if (programTableController != null) {
            programTableController.clearDebugHighlight();
        }

        debugging.set(false);
        debugPaused.set(false);
        running.set(false);

        // Clear change highlighting
        previousVariableValues.clear();
        updateResultsDisplay();
    }

    private void finishDebugging() {
        debugging.set(false);
        debugPaused.set(false);

        // Update history table
        if (historyController != null) {
            historyController.showHistory(engine.getHistory());
        }

        // Clear change highlighting
        previousVariableValues.clear();
        updateResultsDisplay();
    }

    private List<VariableDTO> prepareInputs() {
        List<VariableDTO> inputs = new ArrayList<>();
        for (VariableDTO v : inputVars) {
            long value = editedValues.getOrDefault(v.getName(), v.getValue());
            inputs.add(new VariableDTO(v.getType(), v.getNum(), value));
        }
        return inputs;
    }

    private void updateResultsDisplay() {
        if (engine == null) return;

        List<VariableDTO> allVariables = new ArrayList<>();
        List<List<VariableDTO>> varByType = engine.getVarByType();

        // Add in order: Y (output), X (input), Z (temp)
        for (List<VariableDTO> vars : varByType) {
            allVariables.addAll(vars);
        }

        resultsList.getItems().clear();
        for (VariableDTO var : allVariables) {
            resultsList.getItems().add(var.getName() + " = " + var.getValue());
        }
    }

    private void storeCurrentVariableState() {
        if (engine == null) return;

        previousVariableValues.clear();
        List<List<VariableDTO>> varByType = engine.getVarByType();
        for (List<VariableDTO> vars : varByType) {
            for (VariableDTO var : vars) {
                previousVariableValues.put(var.getName(), var.getValue());
            }
        }
    }

    private boolean isVariableChanged(String varName) {
        if (!debugging.get() || previousVariableValues.isEmpty()) {
            return false;
        }

        // Get current value
        List<List<VariableDTO>> varByType = engine.getVarByType();
        for (List<VariableDTO> vars : varByType) {
            for (VariableDTO var : vars) {
                if (var.getName().equals(varName)) {
                    Long previousValue = previousVariableValues.get(varName);
                    return previousValue != null && !previousValue.equals(var.getValue());
                }
            }
        }
        return false;
    }

    // Setters and getters
    public void setEngine(EngineImpl engine) {
        this.engine = engine;
    }

    public void setHistoryController(HistoryTableController historyController) {
        this.historyController = historyController;
    }

    public void setProgramTableController(ProgramTableController programTableController) {
        this.programTableController = programTableController;
    }

    public void setCurrentDegree(int degree) {
        this.currentDegree = degree;
    }

    public void loadInputVariables() {
        Platform.runLater(() -> {
            resetToInitialState();

            List<VariableDTO> inputs = Collections.emptyList();
            if (engine != null && engine.isLoaded()) {
                inputs = engine.getInputs();
            }

            ObservableList<VariableDTO> newItems = FXCollections.observableArrayList(inputs);
            inputsTable.setItems(newItems);
            inputVars.setAll(newItems);

            inputsTable.getColumns().clear();
            inputsTable.getColumns().addAll(varColumn, valueColumn);
            inputsTable.getSelectionModel().clearSelection();
            inputsTable.setPlaceholder(new Label("No input variables"));
            inputsTable.refresh();
        });
    }

    // Property getters for binding
    public BooleanProperty runningProperty() { return running; }
    public BooleanProperty debuggingProperty() { return debugging; }
    public IntegerProperty debugLineProperty() { return debugLine; }
    public IntegerProperty currentCyclesProperty() { return currentCycles; }
}