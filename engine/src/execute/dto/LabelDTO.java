package execute.dto;

import logic.labels.Label;

public class LabelDTO {
    private final String name;

    public LabelDTO(String name) {
        this.name = name;
    }

    public LabelDTO(Label label) {
        this.name = label.getLabel();
    }

    public String getLabel() { return name; }
}
