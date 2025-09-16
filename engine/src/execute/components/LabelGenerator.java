package execute.components;

import logic.instructions.Instruction;
import logic.labels.Label;
import logic.labels.NumericLabel;

import java.util.*;

public class LabelGenerator {
    private int maxLabel;
    private Map<String, Label> labels;

    public LabelGenerator() {
        this.labels = new HashMap<>();
        this.maxLabel = 0;
    }

    public List<Label> getLabels() {
        return new ArrayList<>(labels.values());
    }

    public void addLabel(Label label) {
        if (label.getClass().equals(NumericLabel.class) && !labels.containsKey(label.getLabel())) {
            labels.put(label.getLabel(), label);
            if (maxLabel <= label.getNum()) {
                maxLabel = label.getNum();
            }
        }
    }

    public void loadInstructionLabels(List<Instruction> instructions) {
        instructions
                .stream()
                .filter(instr -> instr.getSelfLabel() instanceof NumericLabel)
                .forEach(instr -> { this.addLabel(instr.getSelfLabel()); } );

        maxLabel = labels.values().stream()
                .max(Comparator.comparing(Label::getNum))
                .map(Label::getNum)
                .orElse(0);
    }

    public void clear() {
        labels.clear();
        maxLabel = 0;
    }

    public void removeLabel(Label label) {
        labels.remove(label.getLabel());
        if (maxLabel == label.getNum()) {
            maxLabel = labels.values().stream()
                    .max(Comparator.comparing(Label::getNum))
                    .map(Label::getNum)
                    .orElse(0);
        }
    }

    public Label newLabel() {
        maxLabel++;
        Label newLabel = new NumericLabel(maxLabel);
        this.addLabel(newLabel);
        return newLabel;
    }
}
