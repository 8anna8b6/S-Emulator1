package execute;

import execute.dto.VariableDTO;
import logic.labels.Label;
import logic.variables.Variable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Engine {
    boolean loadFromXML(String filePath);

    void printProgram(int degree);

    long runProgram(int degree);

    long runProgramAndRecord(int degree, List<Long> inputs);

    void resetVars();

    boolean validateProgram(int degree);

    int maxDegree();

    List<List<VariableDTO>> getVarByType();

    List<VariableDTO> getInputs();

    int getCycles(int degree);

    void fillOutVars(Map<String, Variable> vars);

    void loadInputs(List<VariableDTO> inputVars);
}
