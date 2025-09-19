package execute.components;

import logic.program.Program;

import java.util.HashMap;
import java.util.Map;

public class ProgramRepository {
    private static final Map<String, Program> repo = new HashMap<>();

    public static void register(Program p) {
        if (p != null && p.getName() != null) {
            repo.put(p.getName(), p);
        }
    }

    public static Program get(String name) {
        return repo.get(name);
    }

    public static void clear() {
        repo.clear();
    }
}

