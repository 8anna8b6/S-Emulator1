package logic.instructions.api.synthetic;

import execute.dto.VariableDTO;
import logic.instructions.Instruction;
import logic.instructions.InstructionData;
import logic.instructions.api.AbstractInstruction;
import logic.labels.FixedLabel;
import logic.labels.Label;
import logic.variables.Variable;

import java.util.List;

/**
 * QUOTE instruction:
 * Represents embedding another function (program Q) inside the current one (program P).
 * In XML this appears as:
 *
 * <S-Instruction type="synthetic" name="QUOTE">
 *   <S-Variable>z1</S-Variable>
 *   <S-Instruction-Arguments>
 *     <S-Instruction-Argument name="functionName" value="Const"/>
 *     <S-Instruction-Argument name="functionArguments" value=""/>
 *   </S-Instruction-Arguments>
 * </S-Instruction>
 *
 * At runtime this does not directly execute Q here.
 * Instead it acts as a placeholder instruction, and the composition/expansion phase
 * is expected to inline Q into P.
 */
public class Quote extends AbstractInstruction {

    private final String functionName;
    private final String functionArguments; // e.g. "x1,x2,..."
    private final Variable target;          // the left-hand-side variable V

    public Quote(Label selfLabel,
                 Variable target,
                 String functionName,
                 String functionArguments,
                 int num,
                 Instruction parent) {
        super(InstructionData.QUOTE, selfLabel, num, parent);
        this.functionName = functionName;
        this.functionArguments = functionArguments == null ? "" : functionArguments;
        this.target = target;
    }

    public Quote(Label selfLabel, Variable target, String functionName, String functionArguments, int num) {
        this(selfLabel, target, functionName, functionArguments, num, null);
    }

    public Quote(Label selfLabel, Variable target, String functionName, String functionArguments) {
        this(selfLabel, target, functionName, functionArguments, 1);
    }

    // ------------------ API ------------------

    public String getFunctionName() { return functionName; }
    public String getFunctionArguments() { return functionArguments; }
    public Variable getTarget() { return target; }

    @Override
    public List<VariableDTO> getVarsDTO() {
        return target != null ? List.of(new VariableDTO(target)) : List.of();
    }

    @Override
    public List<Variable> getVars() {
        return target != null ? List.of(target) : List.of();
    }

    @Override
    public int getConst() {
        // QUOTE is not a constant-producing instruction
        return 0;
    }

    @Override
    public Label execute() {
        // QUOTE itself is non-executable: expansion phase replaces it.
        // At runtime, just return EMPTY (no control-flow change).
        return FixedLabel.EMPTY;
    }

    @Override
    public String print() {
        return String.format(
                "%s <- (%s,%s)",
                target != null ? target.getName() : "?",
                functionName,
                functionArguments
        );
    }
}
