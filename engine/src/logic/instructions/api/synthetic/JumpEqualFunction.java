package logic.instructions.api.synthetic;

import execute.dto.VariableDTO;
import logic.instructions.Instruction;
import logic.instructions.InstructionData;
import logic.instructions.api.AbstractInstruction;
import logic.labels.FixedLabel;
import logic.labels.Label;
import logic.program.Program;
import logic.variables.Variable;
import logic.variables.VariableType;

import java.util.*;

public class JumpEqualFunction extends AbstractInstruction {

    private final Variable v;
    private final Variable z1;
    private final String functionName;
    private final String functionArguments;
    private final Label jumpLabel;

    public JumpEqualFunction(Label selfLabel,
                             Variable v,
                             Variable z1,
                             String functionName,
                             String functionArguments,
                             Label jumpLabel,
                             int num,
                             Instruction parent) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, selfLabel, num, parent);
        this.v = v;
        this.z1 = z1;
        this.functionName = functionName;
        this.functionArguments = functionArguments;
        this.jumpLabel = jumpLabel;
    }

    public JumpEqualFunction(Label selfLabel,
                             Variable v,
                             Variable z1,
                             String functionName,
                             String functionArguments,
                             Label jumpLabel,
                             int num) {
        this(selfLabel, v, z1, functionName, functionArguments, jumpLabel, num, null);
    }

    public JumpEqualFunction(Label selfLabel,
                             Variable v,
                             Variable z1,
                             String functionName,
                             String functionArguments,
                             Label jumpLabel) {
        this(selfLabel, v, z1, functionName, functionArguments, jumpLabel, 1, null);
    }

    @Override
    public List<VariableDTO> getVarsDTO() {
        return List.of(new VariableDTO(v), new VariableDTO(z1));
    }

    @Override
    public List<Variable> getVars() {
        return List.of(v, z1);
    }

    public Variable getV() { return v; }
    public Variable getTemp() { return z1; }
    public String getFunctionName() { return functionName; }
    public String getFunctionArguments() { return functionArguments; }
    public Label getJumpLabel() { return jumpLabel; }

    @Override
    public Label execute() {

        if (v.getValue() == z1.getValue()) {
            return jumpLabel;
        } else {
            return FixedLabel.EMPTY;
        }
    }

    @Override
    public String print() {
        return "IF " + v.getName() + " = (" + functionName + "," + functionArguments + ") GOTO " + jumpLabel.getLabel();
    }

    @Override
    public Label getTargetLabel() {
        return jumpLabel;
    }
}
