package logic.labels;

public enum FixedLabel implements Label {
    EMPTY {
        @Override
        public String getLabel() { return ""; }

        @Override
        public int getNum() { return 0; }
    },
    EXIT {
        @Override
        public String getLabel() { return "EXIT"; }

        @Override
        public int getNum() { return 0; }
    },
}
