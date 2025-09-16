package logic.instructions.api.synthetic;

import execute.dto.VariableDTO;
import logic.instructions.Instruction;
import logic.instructions.InstructionData;
import logic.instructions.api.AbstractInstruction;
import logic.labels.FixedLabel;
import logic.labels.Label;
import logic.variables.Variable;

import java.util.List;


public class Assignment extends AbstractInstruction {

    private final Variable x, y;

    public Assignment(Label selfLabel, Variable x, Variable y, int num, Instruction parent) {
        super(InstructionData.ASSIGNMENT,  selfLabel, num, parent);
        this.x = x;
        this.y = y;
    }

    public Assignment(Label selfLabel, Variable x, Variable y, int num) {
        this(selfLabel, x, y, num, null);
    }

    public Assignment(Label selfLabel, Variable x, Variable y) {
        this(selfLabel, x, y, 1);
    }

    @Override
    public List<VariableDTO> getVarsDTO() { return List.of(new VariableDTO(x), new VariableDTO(y)); }

    @Override
    public List<Variable> getVars() { return List.of(x, y); }

    @Override
    public Label execute() {
        x.setValue(y.getValue());
        return FixedLabel.EMPTY;
    }

    @Override
    public String print() {
        return x.getName() + " <- " + y.getName();
    }
    public Variable getX() { return x; }
    public Variable getY() { return y; }
}