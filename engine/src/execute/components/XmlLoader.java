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

    public static Program parse(String filePath, Map<String, Variable> varsMap) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Error: File does not exist.");
            return null;
        }
        if (!filePath.toLowerCase().endsWith(".xml")) {
            System.out.println("Error: File is not an XML file.");
            return null;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            List<Instruction> instructions = new ArrayList<>();
            Map<Label, Instruction> labels = new HashMap<>();

            String programName = doc.getDocumentElement().getAttribute("name");

            NodeList instrNodes = doc.getElementsByTagName("S-Instruction");

            System.out.println("Found " + instrNodes.getLength() + " instructions in XML");

            for (int i = 0; i < instrNodes.getLength(); i++) {
                Element instrElem = (Element) instrNodes.item(i);

                String instrName = instrElem.getAttribute("name");
                String type = instrElem.getAttribute("type");

                // variable (main one if exists)
                String varName = "";
                Variable var = null;
                NodeList varNodes = instrElem.getElementsByTagName("S-Variable");
                if (varNodes.getLength() > 0) {
                    varName = varNodes.item(0).getTextContent().trim();
                    if (!varsMap.containsKey(varName)) {
                        varsMap.put(varName, new Var(varName));
                    }
                    var = varsMap.get(varName);
                }

                // label of the instruction
                Label selfLabel = FixedLabel.EMPTY;
                NodeList labelNodes = instrElem.getElementsByTagName("S-Label");
                if (labelNodes.getLength() > 0) {
                    String labelName = labelNodes.item(0).getTextContent().trim();
                    selfLabel = parseLabel(labelName);
                }

                // collect all arguments into a map
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

                // some instructions have a target label directly
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
                    System.out.println("Unknown instruction name: " + instrName + " (type=" + type + ")");
                }
            }

            Program program = new SProgram(programName, labels, instructions);

            if (!program.checkLabels()) {
                System.out.println("Error: Program has invalid labels.");
                return null;
            }
            else return program;

        } catch (Exception e) {
            System.out.println("Error parsing XML: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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

            // ---- Synthetic ----
            case "ZERO_VARIABLE" -> new ZeroVariable(selfLabel, var);

            case "GOTO_LABEL" -> {
                String lbl = args.get("gotoLabel");
                Label tgt = parseLabel(lbl);
                yield new GoToLabel(selfLabel, tgt);
            }

            case "ASSIGNMENT" -> {
                // main variable from <S-Variable> is target (y)
                String assignedVarName = args.get("assignedVariable");
                Variable targetVar = var; // <S-Variable> → y
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
