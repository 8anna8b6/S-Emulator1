package logic.instructions.api.synthetic;

import execute.dto.LabelDTO;
import execute.dto.VariableDTO;
import logic.instructions.Instruction;
import logic.instructions.InstructionData;
import logic.instructions.api.AbstractInstruction;
import logic.labels.FixedLabel;
import logic.labels.Label;
import logic.variables.Variable;

import java.util.List;

public class JumpEqualVariable extends AbstractInstruction {

    private final Variable x;
    private final Label target;
    private final Variable y;

    public JumpEqualVariable(Label selfLabel, Variable x, Variable y, Label target, int num, Instruction parent) {
        super(InstructionData.JUMP_EQUAL_VARIABLE, selfLabel, num, parent);
        this.x = x;
        this.y = y;
        this.target = target;
    }

    public JumpEqualVariable(Label selfLabel, Variable x, Variable y, Label target, int num) {
        this( selfLabel, x, y, target, num, null);
    }

    public JumpEqualVariable(Label selfLabel, Variable x, Variable y, Label target) {
        this( selfLabel, x, y, target, 1);
    }

    @Override
    public List<VariableDTO> getVarsDTO() { return List.of(new VariableDTO(x),  new VariableDTO(y)); }

    @Override
    public List<Variable> getVars() { return List.of(x, y); }

    @Override
    public LabelDTO getArgLabelDTO() { return new LabelDTO(target.getLabel()); }

    @Override
    public Label execute() {
        if (x.getValue() == y.getValue()) return target;
        else return FixedLabel.EMPTY;
    }

    @Override
    public String print() {
        return "IF " + x.getName() + " = " + y.getName() + " GOTO " + target.getLabel();
    }
    public Variable getVar1() { return x; }
    public Variable getVar2() { return y; }

    @Override
    public Label getTargetLabel() {
        return target;
    }
}
