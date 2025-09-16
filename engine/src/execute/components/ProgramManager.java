package execute.components;

import logic.instructions.Instruction;
import logic.instructions.api.basic.Decrease;
import logic.instructions.api.basic.Increase;
import logic.instructions.api.basic.JumpNotZero;
import logic.instructions.api.basic.Neutral;
import logic.instructions.api.synthetic.*;
import logic.labels.FixedLabel;
import logic.labels.Label;
import logic.program.Program;
import logic.program.SProgram;
import logic.variables.Var;
import logic.variables.Variable;
import logic.variables.VariableType;

import java.util.*;

public class ProgramManager {
    private List<Program> programExpansions;
    private LabelGenerator labelGenerator;
    private Map<String, Variable> tempVarsMap;
    private int currentTemps;
    private int maxDegree;


    public ProgramManager(Map<String, Variable> tempVarsMap) {
        this.labelGenerator = new  LabelGenerator();
        this.programExpansions = new ArrayList<Program>();
        this.tempVarsMap = tempVarsMap;
        this.currentTemps = 0;
        this.maxDegree = 0;
    }

    public void loadNewProgram(Program program) {
        this.clear();
        programExpansions.add(program);
        this.maxDegree = program.maxDegree();
    }

    public void clear() {
        programExpansions.clear();
        labelGenerator.clear();
        currentTemps = tempVarsMap.values()
                .stream().max(Comparator.comparing(Variable::getNum))
                .map(Variable::getNum).orElse(0);
        maxDegree = 0;
    }

    private Variable generateTempVar() {
        currentTemps++;
        Variable newVar = new Var(VariableType.TEMP, currentTemps, 0);
        tempVarsMap.put(newVar.getName(), newVar);
        return newVar;
    }

    public boolean isEmpty() {
        return programExpansions.isEmpty();
    }

    public int maxDegree() {
        return this.maxDegree;
    }

    public Program getProgram(int degree) {
        if  (programExpansions.isEmpty()) {
            return null;
        }
        else {
            assert 0 <= degree && degree <= maxDegree;
            if (degree >= programExpansions.size()) {
                this.expand(degree);
            }
            return programExpansions.get(degree);
        }
    }


    public int getProgramCycles(int degree) {
        if  (programExpansions.isEmpty()) {
            return 0;
        }
        else {
            assert 0 <= degree && degree <= maxDegree;
            if (degree >= programExpansions.size()) {
                this.expand(degree);
            }
            return programExpansions.get(degree).cycles();
        }
    }

    public void printProgram(int degree) {
        assert 0 <= degree && degree <= maxDegree;
        if (!programExpansions.isEmpty()) {
            if (programExpansions.size() <= degree) {
                this.expand(degree);
            }
            programExpansions.get(degree)
                    .getInstructions()
                    .forEach(instr -> { System.out.println(instr.getRepresentation()); } );
        }
    }

    public void runProgram(int degree) {
        assert 0 <= degree && degree <= maxDegree;
        if (!programExpansions.isEmpty()) {
            if (programExpansions.size() <= degree) {
                this.expand(degree);
            }
            programExpansions.get(degree).run();
        }
    }

    private void expand(int degree) {
        assert 0 <= degree && degree <= maxDegree;
        while (degree + 1 > programExpansions.size()) {
            this.expandOnce();
        }
    }

    private void expandOnce() {
        Program currentProgram = programExpansions.getLast();
        List<Instruction> currentInstructions = currentProgram.getInstructions();
        List<Instruction> newInstructions = new ArrayList<>();

        labelGenerator.clear();
        labelGenerator.loadInstructionLabels(currentInstructions);

        for (Instruction instr : currentInstructions) {
            List<Variable> instrVars = instr.getVars();
            for (Variable v : instrVars) {
                if (v.getType() == VariableType.TEMP) {
                    tempVarsMap.put(v.getName(), v);
                }
            }
        }

        int lineNum = 1;
        for (Instruction instr: currentInstructions) {
            List<Instruction> expansion = this.expandInstruction(instr, lineNum);
            lineNum += expansion.size();
            newInstructions.addAll(expansion);
        }

        List<Label> currentLabels = labelGenerator.getLabels();
        Map<Label, Instruction> newLabels = new HashMap<>();
        for (Label label : currentLabels) {
            newInstructions.stream()
                    .filter(instr -> instr.getSelfLabel().equals(label))
                    .findFirst().ifPresent(labeledInstr -> newLabels.put(label, labeledInstr));
        }

        programExpansions.add(new SProgram(currentProgram.getName(), newLabels, newInstructions));
    }

    private List<Instruction> expandInstruction(Instruction instr, int lineNum) {
        List<Instruction> result = new ArrayList<>();
        Label self = instr.getSelfLabel();
        //labelGenerator.addLabel(self);

        // ---- ZERO_VARIABLE ----
        if (instr instanceof ZeroVariable zv) {
            Variable v = zv.getVariable();
            Label loop = labelGenerator.newLabel();
            result.add(new JumpNotZero(self, v, loop, lineNum++, instr));
            result.add(new Decrease(loop, v, lineNum++, instr));
            result.add(new JumpNotZero(FixedLabel.EMPTY, v, loop, lineNum, instr));
        }

        // ---- CONSTANT_ASSIGNMENT ----
        else if (instr instanceof ConstantAssignment ca) {
            Variable v = ca.getVariable();
            int k = ca.getConstant();
            result.add(new ZeroVariable(self, v, lineNum++, instr));
            for (int i = 0; i < k; i++) {
                result.add(new Increase(FixedLabel.EMPTY, v, lineNum++, instr));
            }
        }

        // ---- ASSIGNMENT ----
        else if (instr instanceof Assignment asg) {
            Variable x = asg.getX();
            Variable y = asg.getY();
            Label loop = labelGenerator.newLabel();
            result.add(new ZeroVariable(self, x, lineNum++, instr));
            result.add(new JumpNotZero(FixedLabel.EMPTY, y, loop, lineNum++, instr));
            result.add(new Decrease(loop, y, lineNum++, instr));
            result.add(new Increase(FixedLabel.EMPTY, x, lineNum++, instr));
            result.add(new JumpNotZero(FixedLabel.EMPTY, y, loop, lineNum, instr));
        }

        // ---- GOTO_LABEL ----
        else if (instr instanceof GoToLabel gtl) {
            Label target = gtl.getTargetLabel();
            Variable dummy = this.generateTempVar();
            result.add(new Increase(self, dummy, lineNum++, instr));      // dummy = 1
            result.add(new JumpNotZero(FixedLabel.EMPTY, dummy, target, lineNum, instr));
        }

        // ---- JUMP_ZERO ----
        else if (instr instanceof JumpZero jz) {
            Variable v = jz.getVariable();
            Label target = jz.getTargetLabel();
            Label skip = labelGenerator.newLabel();
            result.add(new JumpNotZero(self, v, skip, lineNum++, instr));
            result.add(new GoToLabel(FixedLabel.EMPTY, target, lineNum++, instr));
            result.add(new Neutral(skip, v, lineNum, instr)); // skip:
        }

        // ---- JUMP_EQUAL_CONSTANT ----
        else if (instr instanceof JumpEqualConstant jec) {
            Variable v = jec.getVariable();
            int k = jec.getConstant();
            Label target = jec.getTargetLabel();

            Variable tmp = this.generateTempVar();
            result.add(new Assignment(self, tmp, v, lineNum++, instr));
            result.add(new ConstantAssignment(FixedLabel.EMPTY, tmp, k, lineNum++, instr));
            // subtract tmp - k loop
            Label loop = labelGenerator.newLabel();
            result.add(new JumpNotZero(FixedLabel.EMPTY, tmp, loop, lineNum++, instr));
            result.add(new Decrease(loop, tmp, lineNum++, instr));
            result.add(new JumpNotZero(FixedLabel.EMPTY, tmp, loop, lineNum++, instr));
            result.add(new JumpZero(FixedLabel.EMPTY, tmp, target, lineNum, instr));
        }

        // ---- JUMP_EQUAL_VARIABLE ----
        else if (instr instanceof JumpEqualVariable jev) {
            Variable v1 = jev.getVar1();
            Variable v2 = jev.getVar2();
            Label target = jev.getTargetLabel();

            Variable t1 = this.generateTempVar();
            Variable t2 = this.generateTempVar();
            result.add(new Assignment(self, t1, v1, lineNum++, instr));
            result.add(new Assignment(FixedLabel.EMPTY, t2, v2, lineNum++, instr));

            Label loop = labelGenerator.newLabel();
            result.add(new JumpNotZero(FixedLabel.EMPTY, t2, loop, lineNum++, instr));
            result.add(new Decrease(loop, t1, lineNum++, instr));
            result.add(new Decrease(FixedLabel.EMPTY, t2, lineNum++, instr));
            result.add(new JumpNotZero(FixedLabel.EMPTY, t2, loop, lineNum++, instr));
            result.add(new JumpZero(FixedLabel.EMPTY, t1, target, lineNum, instr));
        } else {
            result.add(instr);
        }

        return result;
    }

}
