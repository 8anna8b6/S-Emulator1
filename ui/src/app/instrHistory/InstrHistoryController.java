package app.instrHistory;

import execute.dto.InstructionDTO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import logic.instructions.InstructionType;
import logic.labels.FixedLabel;

import java.util.Collections;
import java.util.List;

public class InstrHistoryController {

    @FXML private TableView<InstructionDTO> instrHistoryTable;
    @FXML private TableColumn<InstructionDTO, Integer> colNumber;
    @FXML private TableColumn<InstructionDTO, String> colType;
    @FXML private TableColumn<InstructionDTO, Integer> colCycles;
    @FXML private TableColumn<InstructionDTO, String> colLabel;
    @FXML private TableColumn<InstructionDTO, String> colInstruction;

    private final ObservableList<InstructionDTO> historyList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instrHistoryTable.setItems(historyList);

        colNumber.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getNum()).asObject());
        colType.setCellValueFactory(cell -> {
            InstructionDTO instr = cell.getValue();
            if (instr != null && instr.getData() != null && instr.getData().getInstructionType() != null) {
                return new SimpleStringProperty(
                        instr.getData().getInstructionType() == InstructionType.SYNTHETIC ? "S" : "B"
                );
            }
            return new SimpleStringProperty("");
        });
        colCycles.setCellValueFactory(cell -> {
            if (cell.getValue().getData() != null) {
                return new SimpleIntegerProperty(cell.getValue().getData().getCycles()).asObject();
            }
            return new SimpleIntegerProperty(0).asObject();
        });
        colLabel.setCellValueFactory(cell -> {
            if (cell.getValue().getSelfLabel() != null &&
                    !cell.getValue().getSelfLabel().getLabel().equals(FixedLabel.EMPTY.getLabel())) {
                return new SimpleStringProperty(cell.getValue().getSelfLabel().getLabel());
            }
            return new SimpleStringProperty("");
        });
        colInstruction.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
    }


    public void showHistory(List<InstructionDTO> history) {
        historyList.clear();
        if (history != null && !history.isEmpty()) {
            // Reverse so that the *original* instruction is last
            Collections.reverse(history);
            historyList.setAll(history);
        }
    }
}

