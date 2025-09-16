package app.historyTable;

import execute.components.RunRecord;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.stream.Collectors;

public class HistoryTableController {

    @FXML private TableView<RunRecord> historyTable;
    @FXML private TableColumn<RunRecord, Integer> columnNumber;
    @FXML private TableColumn<RunRecord, Integer> columnDegree;
    @FXML private TableColumn<RunRecord, String> columnInpust;
    @FXML private TableColumn<RunRecord, Integer> columnCycles;
    @FXML private TableColumn<RunRecord, String> columnOutput;

    private final ObservableList<RunRecord> runRecords = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        historyTable.setItems(runRecords);

        columnNumber.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRunId()).asObject());
        columnDegree.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getDegree()).asObject());
        columnInpust.setCellValueFactory(c -> {
            String inputs = c.getValue().getInputs().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(inputs);
        });
        columnOutput.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getResultY())));
        columnCycles.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCycles()).asObject());
    }

    public void showHistory(List<RunRecord> history) {
        for (RunRecord record : history) {
            if (!runRecords.contains(record)) { // avoid duplicates
                runRecords.add(record);
            }
        }
    }
}
