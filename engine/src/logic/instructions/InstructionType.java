package logic.instructions;

public enum InstructionType {
    BASIC {
        @Override
        public String toString() { return "B"; }
    },
    SYNTHETIC {
        @Override
        public String toString() { return "S"; }
    };
}
