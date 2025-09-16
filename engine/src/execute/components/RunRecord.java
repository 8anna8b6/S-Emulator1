package execute.components;

import java.util.List;

public class RunRecord {
    private final int runId;
    private final int degree;
    private final List<Long> inputs;
    private final long resultY;
    private final int cycles;

    public RunRecord(int runId, int degree, List<Long> inputs, long resultY, int cycles) {
        this.runId = runId;
        this.degree = degree;
        this.inputs = inputs;
        this.resultY = resultY;
        this.cycles = cycles;
    }

    public int getRunId() { return runId; }
    public int getDegree() { return degree; }
    public List<Long> getInputs() { return inputs; }
    public long getResultY() { return resultY; }
    public int getCycles() { return cycles; }
}
