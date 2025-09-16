package logic.instructions.api.synthetic;
import execute.dto.VariableDTO;
import logic.instructions.Instruction;
import logic.instructions.InstructionData;
import logic.instructions.api.AbstractInstruction;
import logic.labels.FixedLabel;
import logic.labels.Label;
import logic.variables.Variable;

import java.util.List;


public class ZeroVariable extends AbstractInstruction {

    private final Variable v;

    public ZeroVariable(Label selfLabel, Variable v, int num, Instruction parent) {
        super(InstructionData.ZERO_VARIABLE,  selfLabel, num, parent);
        this.v = v;
    }

    public ZeroVariable(Label selfLabel, Variable v, int num) {
        this(selfLabel, v, num, null);
    }

    public ZeroVariable(Label selfLabel, Variable v) {
        this( selfLabel, v, 1);
    }

    @Override
    public List<VariableDTO> getVarsDTO() { return List.of(new VariableDTO(v)); }

    @Override
    public List<Variable> getVars() { return List.of(v); }

    @Override
    public Label execute() {
        v.setValue(0);
        return FixedLabel.EMPTY;
    }

    @Override
    public String print() {
        return v.getName() + " <- 0";
    }
    public Variable getVariable() { return v; }
}