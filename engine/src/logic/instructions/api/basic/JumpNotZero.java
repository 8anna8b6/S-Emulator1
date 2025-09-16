package logic.instructions.api.basic;
import execute.dto.LabelDTO;
import execute.dto.VariableDTO;
import logic.instructions.Instruction;
import logic.instructions.InstructionData;
import logic.instructions.api.AbstractInstruction;
import logic.labels.FixedLabel;
import logic.labels.Label;
import logic.variables.Variable;

import java.util.List;

public class JumpNotZero extends AbstractInstruction {

    private final Variable v;
    private final Label target;

    public JumpNotZero(Label selfLabel, Variable v, Label target, int num, Instruction parent) {
        super(InstructionData.JUMP_NOT_ZERO, selfLabel, num,  parent);
        this.v = v;
        this.target = target;
    }

    public JumpNotZero(Label selfLabel, Variable v, Label target, int num) {
        this(selfLabel, v, target, num, null);
    }

    public JumpNotZero(Label selfLabel, Variable v, Label target) {
        this(selfLabel, v, target, 1);
    }

    @Override
    public List<VariableDTO> getVarsDTO() { return List.of(new VariableDTO(v)); }

    @Override
    public List<Variable> getVars() { return List.of(v); }

    @Override
    public LabelDTO getArgLabelDTO() { return new LabelDTO(target.getLabel()); }

    @Override
    public Label execute() {
        if (v.getValue() != 0) return target;
        else return FixedLabel.EMPTY;
    }

    @Override
    public String print() {
        return "IF " + v.getName() + " != 0 GOTO " + target.getLabel();
    }

    @Override
    public Label getTargetLabel() {
        return target;
    }
}
