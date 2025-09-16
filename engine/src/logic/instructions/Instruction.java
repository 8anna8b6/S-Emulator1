package logic.instructions;
import execute.dto.LabelDTO;
import execute.dto.VariableDTO;
import logic.labels.Label;
import logic.variables.Variable;

import java.util.List;

public interface Instruction {
    String getRepresentation();

    String print();

    Label execute();

    int getCycles();

    Label getSelfLabel();

    int getDegree();

    void setNum(int num);

    Label getTargetLabel();

    List<Variable> getVars();

    int getNum();

    InstructionData getData();

    List<VariableDTO> getVarsDTO();

    LabelDTO getSelfLabelDTO();

    LabelDTO getArgLabelDTO();

    int getConst();

    Instruction getParent();
}