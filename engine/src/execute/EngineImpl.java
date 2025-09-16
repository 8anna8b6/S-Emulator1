package execute;

import execute.dto.InstructionDTO;
import execute.dto.VariableDTO;
import execute.components.ProgramManager;
import execute.components.RunRecord;
import execute.components.XmlLoader;
import logic.instructions.Instruction;
import logic.program.Program;
import logic.variables.Var;
import logic.variables.Variable;
import logic.variables.VariableType;

import java.util.*;
import java.util.stream.Collectors;

public class EngineImpl implements Engine {
    private Map<String, Variable> inputVarsMap;
    private Map<String, Variable> tempVarsMap;
    private Variable outputVar;
    private ProgramManager pm;
    private final List<RunRecord> history;
    private int runCounter = 0;

    public EngineImpl() {
        this.tempVarsMap = new HashMap<>();
        this.inputVarsMap = new HashMap<>();
        this.pm = new ProgramManager(tempVarsMap);
        this.history = new ArrayList<>();
    }

    public boolean isLoaded() {
        return !pm.isEmpty();
    }

    @Override
    public void resetVars() {
        inputVarsMap.values().forEach(v -> v.setValue(0));
    }

    @Override
    public boolean validateProgram(int degree) {
        return pm.getProgram(degree).checkLabels();
    }

    @Override
    public int maxDegree() {
        return pm.maxDegree();
    }

    @Override
    public List<List<VariableDTO>> getVarByType() {

        List<VariableDTO> xList = inputVarsMap.values() // snapshot
                .stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Variable::getName))
                .map(VariableDTO::new)
                .toList();


        List<VariableDTO> yList = List.of(new VariableDTO(outputVar));

        List<VariableDTO> zList = tempVarsMap.values()
                .stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Variable::getName))
                .map(VariableDTO::new)
                .toList();

        return List.of(yList, xList, zList);
    }

    @Override
    public List<VariableDTO> getInputs() {
        return this.getVarByType().get(1);
    }

    @Override
    public int getCycles(int degree) {
        return pm.getProgram(degree).cycles();
    }

    @Override
    public boolean loadFromXML(String filePath) {
        Map<String, Variable> vars = new HashMap<>();
        Program program = XmlLoader.parse(filePath, vars);
        if (program != null) {
            this.fillOutVars(vars);
            pm.loadNewProgram(program);
            this.history.clear();
            System.out.println("Program '" + program.getName() + "' loaded successfully!");
            return true;
        } else {
            System.out.println("Program not loaded. Keeping previous program.");
            return false;
        }
    }

    @Override
    public void fillOutVars(Map<String, Variable> vars) {
        for (Variable variable : vars.values()) {
            if (variable.getType() == VariableType.INPUT) {
                this.inputVarsMap.put(variable.getName(), variable);
            }
            else if (variable.getType() == VariableType.OUTPUT) {
                this.outputVar = variable;
            }
            else if (variable.getType() == VariableType.TEMP) {
                this.tempVarsMap.put(variable.getName(), variable);
            }
        }
    }

    @Override
    public void loadInputs(List<VariableDTO> inputVarsDTO) {
        for (VariableDTO variableDTO : inputVarsDTO) {
            if (inputVarsMap.containsKey(variableDTO.getName())) {
                inputVarsMap.get(variableDTO.getName()).setValue(variableDTO.getValue());
            }
            else {
                inputVarsMap.put(variableDTO.getName(), new Var(variableDTO));
            }
        }
    }

    @Override
    public void printProgram(int degree) {
        if (pm.isEmpty()) {
            System.out.println("No program loaded.");
            return;
        }
        pm.printProgram(degree);
    }

    public void printHistory() {
        if (history.isEmpty()) {
            System.out.println("No runs recorded yet.");
            return;
        }
        for (RunRecord r : history) {
            System.out.printf("#%d | degree = %d | inputs = %s | y = %d | cycles = %d%n",
                    r.getRunId(), r.getDegree(), r.getInputs(), r.getResultY(), r.getCycles());
        }
    }

    @Override
    public long runProgram(int degree) {
        outputVar.setValue(0);
        tempVarsMap.values()
                .stream()
                .filter(Objects::nonNull)
                .forEach(v -> v.setValue(0));
        pm.runProgram(degree);
        return outputVar.getValue();
    }

    @Override
    public long runProgramAndRecord(int degree, List<Long> inputs) {
        long result = this.runProgram(degree);
        int cycles = pm.getProgramCycles(degree);

        runCounter++;
        history.add(new RunRecord(runCounter, degree, inputs, result, cycles));

        return result;
    }
    public List<InstructionDTO> getInstructionsOfProgram(int degree) {
        if (!isLoaded()) return List.of();
        return pm.getProgram(degree).getInstructions().stream()
                .map(InstructionDTO::new)
                .collect(Collectors.toList());
    }

    public List<InstructionDTO> getExpansionHistory(InstructionDTO dto) {
        List<InstructionDTO> history = new ArrayList<>();
        InstructionDTO current = dto;
        while (current != null) {
            history.add(current);
            current = current.getParent();
        }
        return history;
    }


    public List<RunRecord> getHistory() {
        return new ArrayList<>(history); // return a copy for safety
    }



}
