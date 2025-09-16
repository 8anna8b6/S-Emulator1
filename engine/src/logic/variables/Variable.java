package logic.variables;

public interface Variable {
    VariableType getType();

    public String getName();

    public long getValue();

    public void setValue(long value);

    int getNum();
}
