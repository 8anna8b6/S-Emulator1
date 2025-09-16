package logic.program;

import logic.instructions.Instruction;
import logic.labels.FixedLabel;
import logic.labels.Label;

import java.util.*;

public class SProgram implements Program {
    private String name;
    private List<Instruction> instructions;
    private Map<Label, Instruction> labels;

    public SProgram(String name, Map<Label, Instruction> labels) {
        this.name = name;
        this.instructions = new ArrayList<>();
        this.labels = labels; // labels must map each label to its instruction
    }

    public SProgram(String name, Map<Label, Instruction> labels, List<Instruction> instructions) {
        this.name = name;
        this.instructions = instructions;
        this.labels = labels; // labels must map each label to its instruction

        for (int i = 0; i < instructions.size(); i++) { // number instructions
            instructions.get(i).setNum(i + 1);
        }
    }


    @Override
    public void run() {
        Label nextLabel;
        int pc = 0;

        while (0 <= pc && pc < instructions.size()) {
            Instruction current = instructions.get(pc);
            nextLabel = current.execute();
            //System.out.println("pc = " + pc); // for debugging
            if (FixedLabel.EXIT == nextLabel) {
                break;
            }

            if (FixedLabel.EMPTY != nextLabel) {
                Instruction target = labels.get(nextLabel);
                if (target == null) {
                    throw new IllegalStateException("Unknown label: " + nextLabel.getLabel());
                }
                int idx = instructions.indexOf(target);
                if (idx < 0) {
                    throw new IllegalStateException("Label not in instruction list: " + nextLabel.getLabel());
                }
                pc = idx;
            } else {
                pc += 1;
            }
        }
    }

    @Override
    public void addInstruction(Instruction instruction) {
        // the variables in instruction must be the same instances saved in
        // tempVars and inputVars - the provided lists of variables in the program
        Label selfLabel = instruction.getSelfLabel();
        if (selfLabel != FixedLabel.EMPTY) {
            labels.put(selfLabel, instruction);
        }
        instructions.add(instruction);
        instruction.setNum(instructions.size());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public Map<Label, Instruction> getLabels() {
        return labels;
    }

    @Override
    public int maxDegree() {
        return instructions.stream()
                .mapToInt(Instruction::getDegree)
                .max().orElse(0);
    }

    @Override
    public int cycles() {
        return instructions.stream()
                .mapToInt(Instruction::getCycles)
                .sum();
    }

    @Override
    public boolean checkLabels() {
        List<Label> usedLabels = new ArrayList<>();
        for (Instruction instruction : instructions) {
            Label usedLabel = instruction.getTargetLabel();
            if (usedLabel != FixedLabel.EMPTY && usedLabel != FixedLabel.EXIT) {
                usedLabels.add(usedLabel);
            }
        }

        for  (Label label : usedLabels) {
            if (!labels.containsKey(label)) {
                return false;
            }
        }
        return true;
    }

}
