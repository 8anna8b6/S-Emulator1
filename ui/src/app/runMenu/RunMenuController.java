package app.runMenu;

import execute.EngineImpl;
import execute.components.RunRecord;
import execute.dto.VariableDTO;
import app.historyTable.HistoryTableController;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;

import java.util.*;

public class RunMenuController {

    @FXML public VBox vBox;
    @FXML private TableView<VariableDTO> inputsTable;
    @FXML private TableColumn<VariableDTO, String> varColumn;
    @FXML private TableColumn<VariableDTO, Long> valueColumn;
    @FXML private ListView<String> resultsList;
    @FXML private TextArea console;
    @FXML private Button runButton;

    // Reference to HistoryTableController
    private HistoryTableController historyController;

    private EngineImpl engine;

    private final ObservableList<VariableDTO> inputVars = FXCollections.observableArrayList();
    private final ReadOnlyListWrapper<VariableDTO> actualInputVariables = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final Map<String, Long> editedValues = new HashMap<>();
    private final BooleanProperty running = new SimpleBooleanProperty(false);

    // Track the degree to run
    private int currentDegree = 0;

    @FXML
    public void initialize() {
        inputsTable.setItems(inputVars);
        inputsTable.setEditable(true);
        inputsTable.getColumns().setAll(varColumn, valueColumn);

        // Configure columns
        varColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        varColumn.setStyle("-fx-alignment: CENTER;");
        valueColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getValue()));

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

        // Run button
        runButton.setOnAction(event -> runProgram());
    }

    /** Set the engine */
    public void setEngine(EngineImpl engine) {
        this.engine = engine;
        loadInputVariables();
    }

    /** Set history controller to update history table */
    public void setHistoryController(HistoryTableController historyController) {
        this.historyController = historyController;
    }

    /** Set degree to run */
    public void setCurrentDegree(int degree) {
        this.currentDegree = degree;
    }

    /** Load input variables from engine */
    public void loadInputVariables() {
        inputVars.clear();
        resultsList.getItems().clear();
        console.clear();
        if (engine != null && engine.isLoaded()) {
            inputVars.addAll(engine.getInputs());
        }
    }

    /** Rebuild ActualInputVariables from table edits */
    private void rebuildInputsFromTable() {
        List<VariableDTO> rebuilt = new ArrayList<>();
        for (VariableDTO v : inputVars) {
            long val = editedValues.getOrDefault(v.getName(), v.getValue());
            rebuilt.add(new VariableDTO(v.getType(), v.getNum(), val));
        }
        actualInputVariables.clear();
        actualInputVariables.setAll(rebuilt);
    }

    /** Run the program */
    private void runProgram() {
        if (engine == null || !engine.isLoaded()) {
            console.appendText("No program loaded.\n");
            return;
        }

        rebuildInputsFromTable();
        List<VariableDTO> inputsToLoad = new ArrayList<>(actualInputVariables);

        engine.loadInputs(inputsToLoad);

        try {
            // Run at the current degree
            long result = engine.runProgramAndRecord(currentDegree,
                    inputsToLoad.stream().map(VariableDTO::getValue).toList());

            // Collect all output variables
            List<VariableDTO> allOutputs = new ArrayList<>();
            List<List<VariableDTO>> varByType = engine.getVarByType();
            for (List<VariableDTO> vars : varByType) {
                allOutputs.addAll(vars);
            }

            // Display outputs
            resultsList.getItems().clear();
            for (VariableDTO var : allOutputs) {
                resultsList.getItems().add(var.getName() + " = " + var.getValue());
            }

            // Total cycles and degree from last run record
            List<RunRecord> history = engine.getHistory();
            if (!history.isEmpty()) {
                RunRecord lastRun = history.get(history.size() - 1);
                console.appendText("Program ran successfully with degree " + lastRun.getDegree() + "\n");
                console.appendText("Total number of cycles: " + lastRun.getCycles() + "\n");
            }

            // Update history table
            if (historyController != null) {
                historyController.showHistory(history);
            }

        } catch (Exception e) {
            console.appendText("Error running program: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    public ReadOnlyListProperty<VariableDTO> actualInputVariablesProperty() {
        return actualInputVariables.getReadOnlyProperty();
    }

    public BooleanProperty runningProperty() {
        return running;
    }
}
