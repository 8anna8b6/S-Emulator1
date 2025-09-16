package logic.variables;

import execute.dto.VariableDTO;

import java.util.Locale;

public class Var implements Variable {
    private int num;
    private long value;
    private VariableType type;
    private String name;

    public Var(VariableType type, int num, long value) {
        if (num < 0 || value < 0) {
            throw new IllegalArgumentException("Num or value of new Var is negative.");
        }
        this.type = type;
        this.num = num;
        this.value = value;
        switch(type) {
            case INPUT:
                name = "x" + num;
                break;
            case TEMP:
                name = "z" + num;
                break;
            case OUTPUT:
                name = "y";
        }
    }

    public Var(VariableType type, int num) {
        this(type, num, 0);
    }

    public Var(VariableDTO dto) { this(dto.getType(), dto.getNum(), dto.getValue()); }

    public Var(String name) {
        int num = 0;
        if (name.length() > 1) {
            num = Integer.parseInt(name.substring(1));
        }

        switch(name.toLowerCase().charAt(0)){
            case 'x':
                this.type = VariableType.INPUT;
                this.name = String.format("x%d", num);
                break;
            case 'z':
                this.type = VariableType.TEMP;
                this.name = String.format("z%d", num);
                break;
            case 'y':
                this.type = VariableType.OUTPUT;
                this.name = "y";
                break;
        }
        this.value = 0;
        this.num = num;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable v)) return false;
        return java.util.Objects.equals(name, v.getName());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name);
    }


    @Override
    public VariableType getType() { return type; }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getValue() { return value; }

    @Override
    public int getNum() { return num; }

    @Override
    public void setValue(long value) {
        this.value = Math.max(value, 0);
    }
}
