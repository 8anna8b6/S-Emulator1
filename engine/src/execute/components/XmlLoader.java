package execute.components;

import logic.instructions.*;
import logic.instructions.api.basic.*;
import logic.instructions.api.synthetic.*;
import logic.labels.FixedLabel;
import logic.labels.Label;
import logic.labels.NumericLabel;
import logic.program.Program;
import logic.program.SProgram;
import logic.variables.Var;
import logic.variables.Variable;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class XmlLoader {

    public static Program parse(String filePath, Map<String, Variable> varsMap) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        if (!filePath.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("File is not an XML file: " + filePath);
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        List<Instruction> instructions = new ArrayList<>();
        Map<Label, Instruction> labels = new HashMap<>();

        String programName = doc.getDocumentElement().getAttribute("name");

        NodeList instrNodes = doc.getElementsByTagName("S-Instruction");

        for (int i = 0; i < instrNodes.getLength(); i++) {
            Element instrElem = (Element) instrNodes.item(i);

            String instrName = instrElem.getAttribute("name");
            String type = instrElem.getAttribute("type");

            // variable
            Variable var = null;
            NodeList varNodes = instrElem.getElementsByTagName("S-Variable");
            if (varNodes.getLength() > 0) {
                String varName = varNodes.item(0).getTextContent().trim();
                if (!varsMap.containsKey(varName)) {
                    varsMap.put(varName, new Var(varName));
                }
                var = varsMap.get(varName);
            }

            // label
            Label selfLabel = FixedLabel.EMPTY;
            NodeList labelNodes = instrElem.getElementsByTagName("S-Label");
            if (labelNodes.getLength() > 0) {
                String labelName = labelNodes.item(0).getTextContent().trim();
                selfLabel = parseLabel(labelName);
            }

            // args
            Map<String, String> args = new HashMap<>();
            NodeList argParents = instrElem.getElementsByTagName("S-Instruction-Arguments");
            if (argParents.getLength() > 0) {
                Element argsElem = (Element) argParents.item(0);
                NodeList argNodes = argsElem.getElementsByTagName("S-Instruction-Argument");
                for (int j = 0; j < argNodes.getLength(); j++) {
                    Element argElem = (Element) argNodes.item(j);
                    String argName = argElem.getAttribute("name");
                    String argValue = argElem.getAttribute("value");
                    args.put(argName, argValue);
                }
            }

            // direct target label
            Label targetLabel = FixedLabel.EMPTY;
            if (args.containsKey("JNZLabel")) {
                targetLabel = parseLabel(args.get("JNZLabel"));
            }

            Instruction instr = createInstruction(instrName, var, selfLabel, targetLabel, args, varsMap);
            if (instr != null) {
                instructions.add(instr);
                if (selfLabel != FixedLabel.EMPTY) {
                    labels.put(selfLabel, instr);
                }
            } else {
                throw new IllegalArgumentException(
                        "Unknown instruction name: " + instrName + " (type=" + type + ")");
            }
        }

        Program program = new SProgram(programName, labels, instructions);

        // register in repository so other QUOTE can find it
        execute.components.ProgramRepository.register(program);

        if (!program.checkLabels()) {
            throw new IllegalStateException("Program has invalid labels.");
        }
        return program;



    }

    private static Instruction createInstruction(String name,
                                                 Variable var,
                                                 Label selfLabel,
                                                 Label target,
                                                 Map<String, String> args,
                                                 Map<String, Variable> vars) {
        return switch (name.toUpperCase()) {
            case "INCREASE" -> new Increase(selfLabel, var);
            case "DECREASE" -> new Decrease(selfLabel, var);
            case "JUMP_NOT_ZERO", "JNZ" -> new JumpNotZero(selfLabel, var, target);
            case "NEUTRAL", "NO_OP" -> new Neutral(selfLabel, var);
            case "ZERO_VARIABLE" -> new ZeroVariable(selfLabel, var);

            case "GOTO_LABEL" -> {
                String lbl = args.get("gotoLabel");
                Label tgt = parseLabel(lbl);
                yield new GoToLabel(selfLabel, tgt);
            }

            case "ASSIGNMENT" -> {
                String assignedVarName = args.get("assignedVariable");
                Variable targetVar = var;
                Variable sourceVar = vars.computeIfAbsent(assignedVarName, Var::new);
                yield new Assignment(selfLabel, targetVar, sourceVar);
            }

            case "CONSTANT_ASSIGNMENT" -> {
                int k = Integer.parseInt(args.get("constantValue"));
                yield new ConstantAssignment(selfLabel, var, k);
            }

            case "JUMP_ZERO" -> {
                String lbl = args.get("JZLabel");
                Label tgt = parseLabel(lbl);
                yield new JumpZero(selfLabel, var, tgt);
            }

            case "JUMP_EQUAL_CONSTANT" -> {
                String lbl = args.get("JEConstantLabel");
                int k = Integer.parseInt(args.get("constantValue"));
                Label tgt = parseLabel(lbl);
                yield new JumpEqualConstant(selfLabel, var, k, tgt);
            }

            case "JUMP_EQUAL_VARIABLE" -> {
                String otherVarName = args.get("variableName");
                Variable otherVar = vars.computeIfAbsent(otherVarName, Var::new);
                String lbl = args.get("JEVariableLabel");
                Label tgt = parseLabel(lbl);
                yield new JumpEqualVariable(selfLabel, var, otherVar, tgt);
            }
            case "QUOTE" -> {
                String functionName = args.get("functionName");
                String functionArguments = args.get("functionArguments");
                yield new logic.instructions.api.synthetic.Quote(selfLabel, var, functionName, functionArguments);
            }
            case "JUMP_EQUAL_FUNCTION" -> {
                String functionName = args.get("functionName");
                String functionArguments = args.get("functionArguments");
                String lbl = args.get("JEFunctionLabel");
                Label jumpLabel = parseLabel(lbl);
                Variable tempVar = vars.computeIfAbsent("z1", Var::new); // משתנה עבודה z1
                yield new JumpEqualFunction(selfLabel, var, tempVar, functionName, functionArguments, jumpLabel);
            }



            default -> null;
        };
    }

    private static Label parseLabel(String labelValue) {
        if (labelValue == null || labelValue.isEmpty()) return FixedLabel.EMPTY;

        String v = labelValue.trim();
        Label target;
        char c0 = Character.toUpperCase(v.charAt(0));
        switch (c0) {
            case 'L' -> {
                int n = Integer.parseInt(v.substring(1));
                target = new NumericLabel(n);
            }
            case 'E' -> target = FixedLabel.EXIT;
            default -> target = FixedLabel.EMPTY;
        }
        return target;
    }
}
