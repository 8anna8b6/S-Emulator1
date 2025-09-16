package app.programTable;

import execute.EngineImpl;
import execute.dto.InstructionDTO;
import execute.dto.LabelDTO;
import execute.dto.VariableDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import logic.instructions.InstructionType;
import logic.labels.FixedLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProgramTableController {

    @FXML
    private TableView<InstructionDTO> programTable;
    @FXML
    private TableColumn<InstructionDTO, Integer> colNumber;
    @FXML
    private TableColumn<InstructionDTO, String> colType;
    @FXML
    private TableColumn<InstructionDTO, Integer> colCycles;
    @FXML
    private TableColumn<InstructionDTO, String> colLabel;
    @FXML
    private TableColumn<InstructionDTO, String> colInstruction;
    private app.historyTable.HistoryTableController historyTableController;

    private EngineImpl engine;
    private app.instrHistory.InstrHistoryController instrHistoryController;
    private final ObservableList<InstructionDTO> instructionList = FXCollections.observableArrayList();
    private final ObservableList<String> variableNames = FXCollections.observableArrayList();

    private String highlightedVar = null;
    private int currentDegree = 0;

    @FXML
    public void initialize() {
        programTable.setItems(instructionList);

        // Column 1: instruction number
        colNumber.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getNum()).asObject()
        );

        // Column 2: Type S/B
        colType.setCellValueFactory(cellData -> {
            InstructionDTO instr = cellData.getValue();
            if (instr != null && instr.getData() != null && instr.getData().getInstructionType() != null) {
                String type = instr.getData().getInstructionType() == InstructionType.SYNTHETIC ? "S" : "B";
                return new SimpleStringProperty(type);
            }
            return new SimpleStringProperty("");
        });

        // Column 3: Cycles
        colCycles.setCellValueFactory(cellData -> {
            InstructionDTO instr = cellData.getValue();
            if (instr != null && instr.getData() != null) {
                return new SimpleIntegerProperty(instr.getData().getCycles()).asObject();
            }
            return new SimpleIntegerProperty(0).asObject();
        });

        // Column 4: Label
        colLabel.setCellValueFactory(cellData -> {
            InstructionDTO instr = cellData.getValue();
            if (instr != null && instr.getSelfLabel() != null &&
                    !instr.getSelfLabel().getLabel().equals(FixedLabel.EMPTY.getLabel())) {
                return new SimpleStringProperty(instr.getSelfLabel().getLabel());
            }
            return new SimpleStringProperty("");
        });

        // Column 5: Instruction representation
        colInstruction.setCellValueFactory(cellData -> {
            InstructionDTO instr = cellData.getValue();
            if (instr != null && instr.getName() != null) {
                return new SimpleStringProperty(instr.getName());
            }
            return new SimpleStringProperty("");
        });

        // Add row selection listener
        programTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            System.out.println("Selection changed - New selection: " + (newSelection != null ? newSelection.getName() : "null"));
            System.out.println("instrHistoryController: " + instrHistoryController);
            System.out.println("engine: " + engine);

            if (newSelection != null && instrHistoryController != null && engine != null) {
                try {
                    // Get the expansion history for the selected instruction
                    List<InstructionDTO> expansionHistory = engine.getExpansionHistory(newSelection);
                    System.out.println("Expansion history size: " + (expansionHistory != null ? expansionHistory.size() : "null"));

                    if (expansionHistory != null) {
                        for (int i = 0; i < expansionHistory.size(); i++) {
                            System.out.println("  [" + i + "] " + expansionHistory.get(i).getName());
                        }
                    }

                    instrHistoryController.showHistory(expansionHistory);
                } catch (Exception e) {
                    System.err.println("Error getting expansion history: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Conditions not met for showing history");
                if (newSelection == null) System.out.println("  - No selection");
                if (instrHistoryController == null) System.out.println("  - No history controller");
                if (engine == null) System.out.println("  - No engine");
            }
        });
    }

    public void setEngine(EngineImpl engine) {
        this.engine = engine;
        System.out.println("Engine set in ProgramTableController: " + engine);
        refreshTable();
    }

    public void setHistoryTableController(app.historyTable.HistoryTableController controller) {
        this.historyTableController = controller;
        System.out.println("HistoryTableController set in ProgramTableController: " + controller);
    }
    public void setInstrHistoryController(app.instrHistory.InstrHistoryController controller) {
        this.instrHistoryController = controller;
    }

    public void loadProgram(String filePath) {
        if (engine != null) {
            boolean loaded = engine.loadFromXML(filePath);

            Platform.runLater(() -> {
                if (loaded && engine.isLoaded()) {
                    currentDegree = 0; // Reset to base program when loading new file
                    refreshTable();


                } else {
                    clearTable();
                }
            });
        }
    }

    public void refreshTable() {
        if (engine != null && engine.isLoaded()) {
            List<InstructionDTO> instructions = engine.getInstructionsOfProgram(currentDegree);
            if (instructions != null && !instructions.isEmpty()) {
                instructionList.setAll(instructions);
            } else {
                clearTable();
            }
        } else {
            clearTable();
        }
    }

    public void clearTable() {
        instructionList.clear();
        // Clear the instruction history when program table is cleared
        if (instrHistoryController != null) {
            instrHistoryController.showHistory(new ArrayList<>());
        }
    }

    public int getInstructionsCount() {
        return instructionList.size();
    }

    private void refreshVariables() {
        if (engine != null && engine.isLoaded()) {
            // Get X, Y, Z variables
            var yVar = engine.getVarByType().get(0).get(0);  // output Y
            var xVars = engine.getVarByType().get(1);        // inputs X
            var zVars = engine.getVarByType().get(2);        // temporary Z

            // Combine all into a single list
            variableNames.setAll(
                    xVars.stream().map(VariableDTO::getName).toList()
                            .stream().toList() // inputs
            );
            variableNames.addAll(zVars.stream().map(VariableDTO::getName).toList()); // temp
            if (yVar != null) variableNames.add(yVar.getName());                     // output
        } else {
            variableNames.clear();
        }
    }

    public ObservableList<String> getVariableNames() {
        return variableNames;
    }

    public void highlightVariable(String variableOrLabel) {
        this.highlightedVar = variableOrLabel;
        programTable.refresh();
    }

    @FXML
    public void runProgramAction() {
        if (engine == null || !engine.isLoaded()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No program loaded!");
            alert.showAndWait();
            return;
        }

        List<VariableDTO> inputVars = engine.getInputs();
        List<VariableDTO> updatedInputs = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        for (VariableDTO var : inputVars) {
            TextInputDialog dialog = new TextInputDialog("0");
            dialog.setTitle("Run Program");
            dialog.setHeaderText("Enter Input Values");
            dialog.setContentText("Enter value for " + var.getName() + ":");

            Optional<String> result = dialog.showAndWait();
            long value = 0;
            if (result.isPresent()) {
                try {
                    value = Long.parseLong(result.get());
                } catch (NumberFormatException ignored) {
                    value = 0; // fallback to 0
                }
            }

            updatedInputs.add(new VariableDTO(var.getType(), var.getNum(), value));
            values.add(value);
        }

        engine.loadInputs(updatedInputs);
        long output = engine.runProgramAndRecord(currentDegree, values);

        // Update the history table with the latest run records
        if (historyTableController != null && engine != null) {
            historyTableController.showHistory(engine.getHistory());
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Program executed successfully!\nOutput (y) = " + output);
        alert.setHeaderText("Run Complete");
        alert.showAndWait();
    }
    public void expandProgram(int degree) {
        this.currentDegree = degree;
        refreshTable();
    }

    public int getMaxDegree() {
        return engine != null ? engine.maxDegree() : 0;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public List<String> getVariableNamesAndLabels() {
        if (engine == null || !engine.isLoaded()) return List.of();

        // X, Z, Y variables
        var vars = engine.getVarByType();
        List<String> names = FXCollections.observableArrayList();
        if (!vars.isEmpty()) {
            // output Y
            if (!vars.get(0).isEmpty()) names.add(vars.get(0).get(0).getName());
            // inputs X
            names.addAll(vars.get(1).stream().map(VariableDTO::getName).toList());
            // temp Z
            names.addAll(vars.get(2).stream().map(VariableDTO::getName).toList());
        }

        // labels L
        List<String> labels = instructionList.stream()
                .map(InstructionDTO::getSelfLabel)
                .filter(l -> l != null && !l.getLabel().isEmpty())
                .map(LabelDTO::getLabel)
                .toList();

        names.addAll(labels);

        return names;
    }
}