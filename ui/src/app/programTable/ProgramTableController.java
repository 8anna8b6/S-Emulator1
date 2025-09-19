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
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    private app.instrHistory.InstrHistoryController instrHistoryController;
    private EngineImpl engine;

    private final ObservableList<InstructionDTO> instructionList = FXCollections.observableArrayList();
    private final ObservableList<String> variableNames = FXCollections.observableArrayList();

    private String highlightedVar = null;
    private int currentDegree = 0;

    private final PseudoClass HIGHLIGHT = PseudoClass.getPseudoClass("highlight");

    @FXML
    public void initialize() {
        programTable.setItems(instructionList);

        // Columns
        colNumber.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getNum()).asObject());

        colType.setCellValueFactory(cellData -> {
            InstructionDTO instr = cellData.getValue();
            if (instr != null && instr.getData() != null && instr.getData().getInstructionType() != null) {
                return new SimpleStringProperty(
                        instr.getData().getInstructionType() == InstructionType.SYNTHETIC ? "S" : "B"
                );
            }
            return new SimpleStringProperty("");
        });

        colCycles.setCellValueFactory(cellData -> {
            InstructionDTO instr = cellData.getValue();
            if (instr != null && instr.getData() != null) {
                return new SimpleIntegerProperty(instr.getData().getCycles()).asObject();
            }
            return new SimpleIntegerProperty(0).asObject();
        });

        colLabel.setCellValueFactory(cellData -> {
            InstructionDTO instr = cellData.getValue();
            if (instr != null && instr.getSelfLabel() != null &&
                    !instr.getSelfLabel().getLabel().equals(FixedLabel.EMPTY.getLabel())) {
                return new SimpleStringProperty(instr.getSelfLabel().getLabel());
            }
            return new SimpleStringProperty("");
        });

        colInstruction.setCellValueFactory(cellData -> {
            InstructionDTO instr = cellData.getValue();
            if (instr != null && instr.getName() != null) {
                return new SimpleStringProperty(instr.getName());
            }
            return new SimpleStringProperty("");
        });

        // Listener for showing history
        programTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && instrHistoryController != null && engine != null) {
                try {
                    List<InstructionDTO> expansionHistory = engine.getExpansionHistory(newSelection);
                    instrHistoryController.showHistory(expansionHistory);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (instrHistoryController != null) {
                instrHistoryController.showHistory(new ArrayList<>());
            }
        });

        programTable.setRowFactory(tv -> new TableRow<InstructionDTO>() {
            @Override
            protected void updateItem(InstructionDTO item, boolean empty) {
                super.updateItem(item, empty);

                // Always clear inline styles first → let CSS handle defaults
                setStyle("");

                if (!empty && item != null) {
                    boolean match = highlightedVar != null &&
                            ((item.getName() != null && item.getName().contains(highlightedVar)) ||
                                    (item.getSelfLabel() != null && item.getSelfLabel().getLabel().contains(highlightedVar)));

                    if (match) {
                        // Add highlight style class (so CSS handles colors)
                        getStyleClass().add("highlight-row");
                    } else {
                        getStyleClass().remove("highlight-row");
                    }
                } else {
                    getStyleClass().remove("highlight-row");
                }
            }
        });


    }

    public void setEngine(EngineImpl engine) {
        this.engine = engine;
        refreshTable();
    }

    public void setHistoryTableController(app.historyTable.HistoryTableController controller) {
        this.historyTableController = controller;
    }

    public void setInstrHistoryController(app.instrHistory.InstrHistoryController controller) {
        this.instrHistoryController = controller;
    }

    public void loadProgram(String filePath) {
        if (engine != null) {
            boolean loaded = engine.loadFromXML(filePath);
            Platform.runLater(() -> {
                if (loaded && engine.isLoaded()) {
                    currentDegree = 0;
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
                variableNames.setAll(getVariableNamesAndLabels());
            } else {
                clearTable();
            }
        } else {
            clearTable();
        }
    }

    public void clearTable() {
        instructionList.clear();
        if (instrHistoryController != null) {
            instrHistoryController.showHistory(new ArrayList<>());
        }
    }

    public int getMaxDegree() {
        return engine != null ? engine.maxDegree() : 0;
    }

    public ObservableList<String> getVariableNames() {
        return variableNames;
    }

    public void highlightVariable(String variableOrLabel) {
        this.highlightedVar = variableOrLabel;
        Platform.runLater(() -> programTable.refresh());
    }

    public List<String> getVariableNamesAndLabels() {
        if (engine == null || !engine.isLoaded()) return List.of();

        var vars = engine.getVarByType();
        List<String> names = new ArrayList<>();
        if (!vars.isEmpty()) {
            if (!vars.get(0).isEmpty()) names.add(vars.get(0).get(0).getName()); // Y
            names.addAll(vars.get(1).stream().map(VariableDTO::getName).toList()); // X
            names.addAll(vars.get(2).stream().map(VariableDTO::getName).toList()); // Z
        }

        List<String> labels = instructionList.stream()
                .map(InstructionDTO::getSelfLabel)
                .filter(l -> l != null && !l.getLabel().isEmpty())
                .map(LabelDTO::getLabel)
                .toList();

        names.addAll(labels);
        return names;
    }

    public void expandProgram(int degree) {
        this.currentDegree = degree;
        refreshTable();
    }
}
