package execute.dto;

import logic.variables.Variable;
import logic.variables.VariableType;

public class VariableDTO {
    private final VariableType type;
    private final int num;
    private final long value;

    public VariableDTO(VariableType type, int num, long value) {
        this.type = type;
        this.num = num;
        this.value = value;
    }

    public VariableDTO(Variable var) {
        this.type = var.getType();
        this.num = var.getNum();
        this.value = var.getValue();
    }




    public VariableType getType() { return type; }
    public int getNum() { return num; }
    public long getValue() { return value; }

    public String getName() {
        return switch (this.type) {
            case VariableType.INPUT -> "x" + num;
            case VariableType.OUTPUT -> "y";
            case VariableType.TEMP ->  "z" + num;
        };
    }


}
