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
        }
        // ---- QUOTE (Function inlining) ----
        else if (instr instanceof logic.instructions.api.synthetic.Quote qt) {
            String fname = qt.getFunctionName();
            String fargs = qt.getFunctionArguments(); // comma separated
            Variable targetVar = qt.getTarget(); // V

            // find function program
            Program funcProg = execute.components.ProgramRepository.get(fname);
            if (funcProg == null) {
                throw new IllegalStateException("Function/program not found for QUOTE: " + fname);
            }

            // parse provided args to names (may be empty)
            List<String> providedArgs = List.of();
            if (fargs != null && !fargs.isBlank()) {
                providedArgs = Arrays.stream(fargs.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }

            // 1) collect all variables used in function program
            Set<Variable> funcVars = new LinkedHashSet<>();
            for (Instruction fi : funcProg.getInstructions()) {
                funcVars.addAll(fi.getVars());
            }

            // maps from original func var -> new var in host (P)
            Map<String, Variable> varMap = new HashMap<>();
            // We will map inputs in order of appearance to providedArgs
            List<Variable> funcInputVars = new ArrayList<>();
            Variable funcOutputVar = null;

            for (Variable fv : funcVars) {
                if (fv.getType() == VariableType.INPUT) funcInputVars.add(fv);
                else if (fv.getType() == VariableType.OUTPUT) funcOutputVar = fv;
            }

            // create temp variables in host for each func var
            // inputs -> zi (we will initialize them from providedArgs)
            for (Variable inVar : funcInputVars) {
                Variable newTemp = generateTempVar();
                varMap.put(inVar.getName(), newTemp);
            }

            // create temps for other temps in function
            for (Variable fv : funcVars) {
                if (fv.getType() == VariableType.TEMP) {
                    if (!varMap.containsKey(fv.getName())) {
                        Variable newTemp = generateTempVar();
                        varMap.put(fv.getName(), newTemp);
                    }
                }
            }

            // output y -> zy
            Variable zy = null;
            if (funcOutputVar != null) {
                zy = generateTempVar();
                varMap.put(funcOutputVar.getName(), zy);
            }

            // 2) prepare label mapping: collect labels from function
            execute.components.LabelGenerator funcLabelGen = new execute.components.LabelGenerator();
            funcLabelGen.loadInstructionLabels(funcProg.getInstructions());
            List<Label> funcLabels = funcLabelGen.getLabels();
            Map<Label, Label> labelMap = new HashMap<>();
            for (Label l : funcLabels) {
                labelMap.put(l, labelGenerator.newLabel());
            }

            // create Lend label for EXIT replacement
            Label Lend = labelGenerator.newLabel();

            // 3) add initial assignments zi <- Vi
            // providedArgs correspond to funcInputVars in order
            for (int i = 0; i < funcInputVars.size(); i++) {
                Variable origInput = funcInputVars.get(i);
                Variable mapped = varMap.get(origInput.getName());
                String srcName = (i < providedArgs.size()) ? providedArgs.get(i) : "0";
                // src variable could be a variable in host (input or temp); fetch or create
                Variable srcVar = tempVarsMap.getOrDefault(srcName, null);
                if (srcVar == null) {
                    srcVar = tempVarsMap.containsKey(srcName) ? tempVarsMap.get(srcName) : tempVarsMap.get(srcName);
                }
                // If not found in temp map, check inputVars in caller? we don't have direct access here.
                // Simpler approach: try to create a Var placeholder (will be added to tempVarsMap)
                if (srcVar == null) {
                    srcVar = new Var(srcName);
                    tempVarsMap.put(srcName, srcVar);
                }
                // zi <- srcVar  -> implemented as Assignment(mapped, srcVar)
                result.add(new logic.instructions.api.synthetic.Assignment(self, mapped, srcVar, lineNum++, instr));
            }

            // 4) translate and append function instructions
            for (Instruction finstr : funcProg.getInstructions()) {
                Label origSelf = finstr.getSelfLabel();
                Label mappedSelf = labelMap.getOrDefault(origSelf, FixedLabel.EMPTY);
                // handle target label mapping for instructions that have targets
                Label targ = finstr.getTargetLabel();
                Label mappedTarget = labelMap.getOrDefault(targ, targ); // if EXIT, will be handled below

                // replace Exit label usage: if target is EXIT we map to Lend
                if (targ.equals(FixedLabel.EXIT)) {
                    mappedTarget = Lend;
                }

                // replace variables of finstr
                List<Variable> newVars = new ArrayList<>();
                for (Variable fv : finstr.getVars()) {
                    Variable nv = varMap.get(fv.getName());
                    if (nv == null) {
                        // if not mapped — create temp
                        nv = generateTempVar();
                        varMap.put(fv.getName(), nv);
                    }
                    newVars.add(nv);
                }

                // Recreate the instruction by type (pattern similar to createInstruction)
                Instruction newInstr = null;
                if (finstr instanceof logic.instructions.api.basic.Increase) {
                    newInstr = new logic.instructions.api.basic.Increase(mappedSelf, newVars.get(0), lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.basic.Decrease) {
                    newInstr = new logic.instructions.api.basic.Decrease(mappedSelf, newVars.get(0), lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.basic.JumpNotZero) {
                    newInstr = new logic.instructions.api.basic.JumpNotZero(mappedSelf, newVars.get(0), mappedTarget, lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.basic.Neutral) {
                    newInstr = new logic.instructions.api.basic.Neutral(mappedSelf, newVars.get(0), lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.synthetic.ZeroVariable) {
                    newInstr = new logic.instructions.api.synthetic.ZeroVariable(mappedSelf, newVars.get(0), lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.synthetic.GoToLabel) {
                    newInstr = new logic.instructions.api.synthetic.GoToLabel(mappedSelf, mappedTarget, lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.synthetic.Assignment) {
                    // note original Assignment holds (target X, source Y)
                    Variable av = newVars.get(0);
                    Variable bv = newVars.size() > 1 ? newVars.get(1) : av;
                    newInstr = new logic.instructions.api.synthetic.Assignment(mappedSelf, av, bv, lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.synthetic.ConstantAssignment) {
                    int k = finstr.getConst();
                    newInstr = new logic.instructions.api.synthetic.ConstantAssignment(mappedSelf, newVars.get(0), k, lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.synthetic.JumpZero) {
                    newInstr = new logic.instructions.api.synthetic.JumpZero(mappedSelf, newVars.get(0), mappedTarget, lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.synthetic.JumpEqualConstant) {
                    int k = finstr.getConst();
                    newInstr = new logic.instructions.api.synthetic.JumpEqualConstant(mappedSelf, newVars.get(0), k, mappedTarget, lineNum++, instr);
                } else if (finstr instanceof logic.instructions.api.synthetic.JumpEqualVariable) {
                    // expect var1,var2
                    Variable vv1 = newVars.get(0);
                    Variable vv2 = newVars.size() > 1 ? newVars.get(1) : vv1;
                    newInstr = new logic.instructions.api.synthetic.JumpEqualVariable(mappedSelf, vv1, vv2, mappedTarget, lineNum++, instr);
                }
                // ---- JUMP_EQUAL_FUNCTION ----
                else if (instr instanceof JumpEqualFunction jef) {
                    Variable v = jef.getV();
                    Variable temp = jef.getTemp();
                    String functionName = jef.getFunctionName();
                    String functionArguments = jef.getFunctionArguments();
                    Label target = jef.getJumpLabel();

                    // 1) הרצת הפונקציה וכתיבה ל-temp
                    result.add(new logic.instructions.api.synthetic.Quote(self, temp, functionName, functionArguments, lineNum++, instr));

                    // 2) השוואת v עם temp ויצירת קפיצה
                    Label loop = labelGenerator.newLabel();
                    result.add(new JumpEqualVariable(self, v, temp, target, lineNum++, instr));
                }


                else {
                    // fallback: keep original (but replace labels/vars references where possible)
                    newInstr = finstr;
                }

                result.add(newInstr);
            }

            // 5) after function, add assignment zy -> targetVar (if zy exists)
            if (zy != null && targetVar != null) {
                result.add(new logic.instructions.api.synthetic.Assignment(FixedLabel.EMPTY, targetVar, zy, lineNum++, instr));
            }

            // 6) add the Lend label as a neutral instruction to be the join point
            result.add(new logic.instructions.api.basic.Neutral(Lend, zy != null ? zy : new Var("0"), lineNum, instr));
        }


        else {
            result.add(instr);
        }

        return result;
    }

}
