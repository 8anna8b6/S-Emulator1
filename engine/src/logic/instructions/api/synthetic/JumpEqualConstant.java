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

public class JumpEqualConstant extends AbstractInstruction {

    private final Variable v;
    private final Label target;
    private final int k;

    public JumpEqualConstant(Label selfLabel, Variable v, int k, Label target, int num, Instruction parent) {
        super(InstructionData.JUMP_EQUAL_CONSTANT, selfLabel, num, parent);
        this.v = v;
        this.target = target;
        this.k = k;
    }

    public JumpEqualConstant(Label selfLabel, Variable v, int k, Label target, int num) {
        this(selfLabel, v, k, target, num, null);
    }

    public JumpEqualConstant(Label selfLabel, Variable v, int k, Label target) {
        this( selfLabel, v, k, target, 1);
    }

    @Override
    public List<VariableDTO> getVarsDTO() { return List.of(new VariableDTO(v)); }

    @Override
    public List<Variable> getVars() { return List.of(v); }

    @Override
    public LabelDTO getArgLabelDTO() { return new LabelDTO(target.getLabel()); }

    @Override
    public int getConst() { return k; }

    @Override
    public Label execute() {
        if (v.getValue() == k) return target;
        else return FixedLabel.EMPTY;
    }

    @Override
    public String print() {
        return "IF " + v.getName() + " = " + k + " GOTO " + target.getLabel();
    }
    public Variable getVariable() { return v; }
    public int getConstant() { return k; }

    @Override
    public Label getTargetLabel() {
        return target;
    }
}
