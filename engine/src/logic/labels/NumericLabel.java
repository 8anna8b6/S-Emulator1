package logic.labels;

public class NumericLabel implements Label {
    private int num;

    public NumericLabel(int num) {
        if  (num < 0) {
            throw new IllegalArgumentException("Num of new Label is negative.");
        }
        this.num = num;
    }

    public NumericLabel(String name) {
        try {
            this.num = Integer.parseInt(name.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Num of new Label is numeric.");
        }
    }

    @Override
    public String getLabel() { return "L" + num; }

    @Override
    public int getNum() { return num; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumericLabel other)) return false;
        return num == other.num;
    }
    @Override
    public int hashCode() { return Integer.hashCode(num); }

}