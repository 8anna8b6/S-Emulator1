package logic.instructions.api;
import execute.dto.LabelDTO;
import logic.instructions.Instruction;
import logic.instructions.InstructionData;
import logic.labels.FixedLabel;
import logic.labels.Label;

public abstract class AbstractInstruction implements Instruction {
    private final Label selfLabel;
    private final InstructionData data;
    private int num;
    private final Instruction parent;

    public AbstractInstruction(InstructionData data) {
        this(data, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionData data, Label selfLabel, int num, Instruction parent) {
        this.data = data;
        this.selfLabel = selfLabel;
        this.num = num;
        this.parent = parent;
    }

    public AbstractInstruction(InstructionData data, Label selfLabel) {
        this(data, selfLabel, 0, null);
    }

    @Override
    public int getConst() { return 0; }

    @Override
    public LabelDTO getArgLabelDTO() { return new LabelDTO(""); }

    @Override
    public LabelDTO getSelfLabelDTO() { return new LabelDTO(selfLabel.getLabel()); }

    @Override
    public Label getSelfLabel() { return selfLabel; }

    @Override
    public Label getTargetLabel() { return FixedLabel.EMPTY; }

    @Override
    public InstructionData getData() { return data; }

    @Override
    public Instruction getParent() { return parent; }

    @Override
    public String getRepresentation() {
        String result = String.format("#%d (%s) [ %-3s ] %s (%d)",
                num,
                data.getInstructionType(),
                selfLabel.getLabel(),
                this.print(),
                data.getCycles());
        if (parent != null) {
            result += " >>> " + parent.getRepresentation();
        }
        return result;
    }

    @Override
    public String toString() { return this.getRepresentation(); }

    @Override
    public int getCycles() { return data.getCycles(); }

    @Override
    public int getDegree() { return data.getDegree(); }

    @Override
    public void setNum(int num) { this.num = num; }

    @Override
    public int getNum() { return num; }
}
