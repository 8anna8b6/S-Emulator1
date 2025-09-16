package logic.program;

import logic.instructions.Instruction;
import logic.labels.Label;
import logic.variables.Variable;
import java.util.List;
import java.util.Map;

public interface Program {
    public void run();
    void addInstruction(Instruction instruction);

    String getName();
    List<Instruction> getInstructions();
    Map<Label, Instruction> getLabels();

    int maxDegree();
    int cycles();

    boolean checkLabels();
}
