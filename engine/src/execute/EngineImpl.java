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

    // Debug state fields
    private boolean debugMode = false;
    private int debugCurrentLine = 0;
    private int debugCurrentCycles = 0;
    private Program debugProgram = null;
    private List<VariableDTO> debugInputs = null;
    private boolean debugPaused = false;

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
        tempVarsMap.values().forEach(v -> v.setValue(0));
        if (outputVar != null) {
            outputVar.setValue(0);
        }
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
    public List<VariableDTO> getInputs() {
        return this.getVarByType().get(1);
    }

    @Override
    public int getCycles(int degree) {
        return pm.getProgram(degree).cycles();
    }

    @Override
    public boolean loadFromXML(String filePath) {
        try {
            Map<String, Variable> vars = new HashMap<>();
            Program program = XmlLoader.parse(filePath, vars);
            if (program != null) {
                this.fillOutVars(vars);
                pm.loadNewProgram(program);
                this.history.clear();
                // Reset debug state when loading new program
                debugStop();
                System.out.println("Program '" + program.getName() + "' loaded successfully!");
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error loading program: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void fillOutVars(Map<String, Variable> vars) {
        this.inputVarsMap.clear();
        this.tempVarsMap.clear();
        this.outputVar = null;

        for (Variable variable : vars.values()) {
            if (variable.getType() == VariableType.INPUT) {
                this.inputVarsMap.put(variable.getName(), variable);
            } else if (variable.getType() == VariableType.OUTPUT) {
                this.outputVar = variable;
            } else if (variable.getType() == VariableType.TEMP) {
                this.tempVarsMap.put(variable.getName(), variable);
            }
        }
    }

    @Override
    public void loadInputs(List<VariableDTO> inputVarsDTO) {
        for (VariableDTO variableDTO : inputVarsDTO) {
            if (inputVarsMap.containsKey(variableDTO.getName())) {
                inputVarsMap.get(variableDTO.getName()).setValue(variableDTO.getValue());
            } else {
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
        if (outputVar != null) {
            outputVar.setValue(0);
        }
        tempVarsMap.values().forEach(v -> v.setValue(0));
        pm.runProgram(degree);
        return (outputVar != null) ? outputVar.getValue() : 0;
    }

    @Override
    public long runProgramAndRecord(int degree, List<Long> inputs) {
        if (debugMode) {
            // If we're in debug mode, just return current output value
            return (outputVar != null) ? outputVar.getValue() : 0;
        }

        // Normal execution
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
        return new ArrayList<>(history);
    }

    // ===================
    // DEBUG FUNCTIONALITY
    // ===================

    /**
     * Start debugging a program at given degree with specified inputs
     */
    public void debugStart(int degree, List<VariableDTO> inputs) {
        if (pm.isEmpty()) {
            throw new RuntimeException("No program loaded for debugging");
        }

        // Reset variables to initial state
        if (outputVar != null) {
            outputVar.setValue(0);
        }
        tempVarsMap.values().forEach(v -> v.setValue(0));

        // Load the inputs
        loadInputs(inputs);

        // Set up debug state
        debugMode = true;
        debugPaused = true; // Start in paused state
        debugCurrentLine = 0;
        debugCurrentCycles = 0;
        debugProgram = pm.getProgram(degree);
        debugInputs = new ArrayList<>(inputs);

        System.out.println("Debug started for degree " + degree + " with " +
                debugProgram.getInstructions().size() + " instructions");
    }

    /**
     * Execute one debug step with proper program flow handling for GOTO/jumps
     */
    public boolean debugStep(int degree) {
        if (!debugMode || debugProgram == null) {
            return false;
        }

        List<Instruction> instructions = debugProgram.getInstructions();

        // Check if we've reached the end or invalid position
        if (debugCurrentLine >= instructions.size() || debugCurrentLine < 0) {
            debugMode = false;
            return false;
        }

        try {
            // Get the instruction to execute
            Instruction currentInstruction = instructions.get(debugCurrentLine);

            // Store the line we're executing for display (1-based)
            int executingLineNumber = debugCurrentLine + 1;

            // Check if this instruction might cause a jump
            String instructionStr = currentInstruction.toString().toLowerCase();
            boolean isJumpInstruction = instructionStr.contains("goto") ||
                    instructionStr.contains("jgtz") ||
                    instructionStr.contains("jzero");

            // Execute the instruction
            currentInstruction.execute();

            // Update cycles
            debugCurrentCycles += currentInstruction.getCycles();

            System.out.println("Debug: Executed line " + executingLineNumber +
                    ": " + currentInstruction.toString() +
                    " (+" + currentInstruction.getCycles() + " cycles, total: " + debugCurrentCycles + ")");

            // Handle program flow after execution
            if (isJumpInstruction) {
                // For jump instructions, check if program flow changed
                int newLine = checkForJump(currentInstruction, debugCurrentLine, instructions);

                if (newLine != debugCurrentLine + 1) {
                    // Jump occurred
                    debugCurrentLine = newLine;
                    System.out.println("Debug: Jump executed - now at line " + (debugCurrentLine + 1));
                } else {
                    // No jump (condition not met), continue normally
                    debugCurrentLine++;
                }
            } else {
                // Normal instruction, move to next line
                debugCurrentLine++;
            }

            // Check bounds after jump/move
            if (debugCurrentLine >= instructions.size() || debugCurrentLine < 0) {
                debugMode = false;
                System.out.println("Debug: Program execution completed");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.err.println("Debug step error at line " + (debugCurrentLine + 1) + ": " + e.getMessage());
            debugMode = false;
            throw new RuntimeException("Debug step failed", e);
        }
    }

    /**
     * Helper method to determine if a jump occurred and where to jump to
     */
    private int checkForJump(Instruction instruction, int currentLine, List<Instruction> instructions) {
        try {
            // This is a simplified approach. In your actual implementation, you would need to:
            // 1. Check if the jump condition was met (for conditional jumps)
            // 2. Find the target label and return its line number

            String instrStr = instruction.toString();

            // For GOTO instructions, we need to find where they jumped to
            if (instrStr.toLowerCase().contains("goto")) {
                // Try to extract the label and find its position
                return findLabelPosition(instrStr, instructions);
            }

            // For conditional jumps (JGTZ, JZERO), check if condition was met
            if (instrStr.toLowerCase().contains("jgtz") || instrStr.toLowerCase().contains("jzero")) {
                // You would need to check the condition here based on your instruction implementation
                // For now, assume normal progression unless we can detect the jump
                return checkConditionalJump(instruction, instrStr, instructions);
            }

            // Default to next line
            return currentLine + 1;

        } catch (Exception e) {
            // If we can't determine jump target, continue normally
            return currentLine + 1;
        }
    }

    /**
     * Find the position of a label in the instruction list
     */
    private int findLabelPosition(String gotoInstruction, List<Instruction> instructions) {
        // Extract label from GOTO instruction
        // This is simplified - you'll need to adapt based on your instruction format
        String[] parts = gotoInstruction.split("\\s+");
        if (parts.length >= 2) {
            String targetLabel = parts[1]; // Assuming format "GOTO LABEL"

            // Find instruction with this label
            for (int i = 0; i < instructions.size(); i++) {
                Instruction instr = instructions.get(i);
                // Check if this instruction has the target label
                if (hasLabel(instr, targetLabel)) {
                    return i; // Return 0-based index
                }
            }
        }

        // If label not found, continue to next instruction
        return -1; // This will cause program to end
    }

    /**
     * Check if an instruction has a specific label
     */
    private boolean hasLabel(Instruction instruction, String targetLabel) {
        // This depends on your instruction implementation
        // You would check if the instruction has the given label
        // This is a placeholder implementation
        try {
            // If your instruction has a getLabel() method or similar
            return instruction.toString().contains(targetLabel + ":");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handle conditional jump instructions
     */
    private int checkConditionalJump(Instruction instruction, String instrStr, List<Instruction> instructions) {
        // This is where you would check if the condition was met
        // For JGTZ: check if value > 0
        // For JZERO: check if value == 0

        // This is a simplified implementation
        // You would need to implement based on your instruction logic

        try {
            if (instrStr.contains("jgtz")) {
                // Check if the condition was met and jump occurred
                // For now, assume no jump (you need to implement condition checking)
                return -1; // Placeholder - implement actual condition checking
            }

            if (instrStr.contains("jzero")) {
                // Check if the condition was met and jump occurred
                // For now, assume no jump (you need to implement condition checking)
                return -1; // Placeholder - implement actual condition checking
            }
        } catch (Exception e) {
            // If condition checking fails, continue normally
        }

        return -1; // No jump occurred
    }

    /**
     * Get the line that was just executed (1-based for UI display)
     */
    public int getDebugLine() {
        return debugCurrentLine; // This is the current position after execution
    }

    /**
     * Get current cycles in debug mode
     */
    public int getCurrentCycles() {
        if (debugMode) {
            return debugCurrentCycles;
        }
        // If not debugging, get cycles from last run
        if (!history.isEmpty()) {
            return history.get(history.size() - 1).getCycles();
        }
        return 0;
    }

    /**
     * Check if currently in debug mode
     */
    public boolean isDebugging() {
        return debugMode;
    }

    /**
     * Pause debugging (for resume functionality)
     */
    public void debugPause() {
        debugPaused = true;
    }

    /**
     * Resume debugging from paused state
     */
    public void debugResume() {
        debugPaused = false;
    }

    /**
     * Stop debugging and reset debug state
     */
    public void debugStop() {
        debugMode = false;
        debugPaused = false;
        debugCurrentLine = 0;
        debugCurrentCycles = 0;
        debugProgram = null;
        debugInputs = null;
    }

    /**
     * Get current variables during debugging or after execution
     */
    public List<VariableDTO> getOutputs() {
        List<VariableDTO> outputs = new ArrayList<>();

        // Add output variable (Y)
        if (outputVar != null) {
            outputs.add(new VariableDTO(outputVar));
        }

        // Add input variables (X) with current values
        List<VariableDTO> inputList = inputVarsMap.values().stream()
                .sorted(Comparator.comparing(Variable::getName))
                .map(VariableDTO::new)
                .collect(Collectors.toList());
        outputs.addAll(inputList);

        // Add temp variables (Z) with current values
        List<VariableDTO> tempList = tempVarsMap.values().stream()
                .sorted(Comparator.comparing(Variable::getName))
                .map(VariableDTO::new)
                .collect(Collectors.toList());
        outputs.addAll(tempList);

        return outputs;
    }

    /**
     * Enhanced getVarByType that reflects current state during debugging
     */
    @Override
    public List<List<VariableDTO>> getVarByType() {
        List<VariableDTO> yList = (outputVar != null)
                ? List.of(new VariableDTO(outputVar))
                : List.of();

        List<VariableDTO> xList = inputVarsMap.values()
                .stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Variable::getName))
                .map(VariableDTO::new)
                .toList();

        List<VariableDTO> zList = tempVarsMap.values()
                .stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Variable::getName))
                .map(VariableDTO::new)
                .toList();

        return List.of(yList, xList, zList);
    }

    /**
     * Get program name for display purposes
     */
    public String getProgramName() {
        return pm.isEmpty() ? "No Program" : pm.getProgram(0).getName();
    }

    /**
     * Get the inputs that are actually used by the program
     */
    public List<VariableDTO> getUsedInputs() {
        if (pm.isEmpty()) {
            return Collections.emptyList();
        }

        // For now, return all input variables
        // You can enhance this to analyze which inputs are actually referenced
        return getInputs();
    }
}