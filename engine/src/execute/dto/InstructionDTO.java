package execute.dto;

import logic.instructions.Instruction;
import logic.instructions.InstructionData;
import java.util.List;

public class InstructionDTO {
    private final LabelDTO selfLabel;
    private final InstructionData data;
    private final int num;
    private final int k;
    private final List<VariableDTO> variables;
    private final LabelDTO argLabel;
    private final InstructionDTO parent;
    private final String name;

    public InstructionDTO(Instruction instr) {
        this.selfLabel = instr.getSelfLabelDTO();
        this.data = instr.getData();
        this.num = instr.getNum();
        this.k = instr.getConst();
        this.variables = instr.getVarsDTO();
        this.argLabel = instr.getArgLabelDTO();

        Instruction localParent = instr.getParent();
        this.parent = localParent == null ? null : new InstructionDTO(localParent);
        this.name = instr.print();
    }

    public LabelDTO getSelfLabel() { return selfLabel; }
    public InstructionData getData() { return data; }
    public int getNum() { return num; }
    public int getConst() { return k; }
    public List<VariableDTO> getVariables() { return variables; }
    public LabelDTO getArgLabel() { return argLabel; }
    public InstructionDTO getParent() { return parent; }
    public String getName() { return name; }
}
