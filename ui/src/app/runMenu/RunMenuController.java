package app.runMenu;

import execute.EngineImpl;
import execute.dto.VariableDTO;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.*;

public class RunMenuController {

    @FXML public VBox vBox;
    @FXML private TableView<VariableDTO> inputsTable;
    @FXML private TableColumn<VariableDTO, String> varColumn;
    @FXML private TableColumn<VariableDTO, Long> valueColumn;
    @FXML private ListView<String> resultsList;
    @FXML private TextArea console;
    @FXML private Button runButton;

    private EngineImpl engine;

    private final ObservableList<VariableDTO> inputVars = FXCollections.observableArrayList();
    private final ReadOnlyListWrapper<VariableDTO> actualInputVariables = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final Map<String, Long> editedValues = new HashMap<>();
    private final BooleanProperty running = new SimpleBooleanProperty(false);

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

    public void setEngine(EngineImpl engine) {
        this.engine = engine;
        loadInputVariables();
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

    /** Run the program using the edited input values */
    private void runProgram() {
        if (engine == null || !engine.isLoaded()) {
            console.appendText("No program loaded.\n");
            return;
        }

        rebuildInputsFromTable();
        List<VariableDTO> inputsToLoad = new ArrayList<>(actualInputVariables);

        engine.loadInputs(inputsToLoad);

        try {
            int degree = engine.maxDegree();
            long result = engine.runProgramAndRecord(degree,
                    inputsToLoad.stream().map(VariableDTO::getValue).toList());

            // Get output variable (Y)
            List<VariableDTO> outputs = new ArrayList<>();
            if (!engine.getVarByType().isEmpty() && !engine.getVarByType().get(0).isEmpty()) {
                outputs.add(engine.getVarByType().get(0).get(0));
            }

            // Display outputs
            resultsList.getItems().clear();
            for (VariableDTO var : outputs) {
                resultsList.getItems().add(var.getName() + " = " + var.getValue());
            }

            console.appendText("Program ran successfully with degree " + degree + "\n");

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
