package console;

import execute.EngineImpl;
import execute.dto.VariableDTO;
import java.util.*;
import java.util.stream.Collectors;

public class ConsoleUI {
    private final EngineImpl engine = new EngineImpl();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new ConsoleUI().mainMenu();
    }

    private void mainMenu() {
        while (true) {
            System.out.println("\n==== S-Emulator ====");
            System.out.println("1. Load Program from XML");
            System.out.println("2. Show Program");
            System.out.println("3. Expand Program");
            System.out.println("4. Run Program");
            System.out.println("5. Show History/Statistics");
            System.out.println("6. Exit");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine().trim();



            switch (choice) {
                case "1" -> loadXml();
                case "2" -> engine.printProgram(0);
                case "3" -> expandProgram();
                case "4" -> runProgram();
                case "5" -> engine.printHistory();
                case "6" -> {
                    System.out.println("Exiting S-Emulator");
                    return;
                }
                default -> System.out.println("Invalid choice, please try again.");
            }
            System.out.print('\n');
        }
    }

    private void loadXml() {
        System.out.print("Enter full XML file path: ");
        String path = scanner.nextLine();
        engine.loadFromXML(path);
    }


    private void expandProgram() {
        if (engine.isLoaded()) {
            int degree = engine.maxDegree();
            System.out.printf("Program maximum degree is: %d.%n", degree);
            System.out.print("Please choose expansion degree (0.." + degree + "): ");

            int chosen;
            try {
                chosen = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
                return;
            }

            if (chosen < 0 || chosen > degree) {
                System.out.println("Invalid degree, please try again.");
            } else {
                engine.printProgram(chosen);
            }
        }
        else System.out.println("Invalid choice: no program loaded.");
    }


    private void runProgram() {
        if (engine.isLoaded()) {
            int maxDegree = engine.maxDegree();
            System.out.printf("Current Program maximum degree is: %d.%n", maxDegree);
            System.out.printf("Please choose Program degree from 0 to %d:%n", maxDegree);

            int degree;
            try {
                degree = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice, please try again.");
                return;
            }

            if (0 <= degree && degree <= maxDegree) {
                List<VariableDTO> requiredInputVars = engine.getInputs();
                System.out.println("The current program's input variables are:");
                requiredInputVars.forEach(s -> System.out.print(s.getName() + " "));
                System.out.println('\n' + "Please enter inputs separated by commas:");
                String input = scanner.nextLine();
                List<Long> inputNumbers;
                try {
                    if (!input.isEmpty()) {
                        inputNumbers = Arrays.stream(input.split(","))
                                .map(String::trim)          // remove spaces
                                .map(Long::parseLong)     // convert to int
                                .collect(Collectors.toList());

                    } else inputNumbers = new ArrayList<>();
                } catch (Exception e) {
                    System.out.println("Invalid input, please try again.");
                    return;
                }

                List<VariableDTO> inputVars = new ArrayList<>();
                List<Integer> inputNumbersIndexes = new ArrayList<>();
                for (int i = 1; i <= requiredInputVars.getLast().getNum(); i++) {
                    if (i <= inputNumbers.size()) {
                        for (VariableDTO var : requiredInputVars) {
                            if (var.getNum() == i) {
                                long value = inputNumbers.get(i - 1);
                                if (value <= 0) {
                                    System.out.println("Invalid input. Please try again.");
                                    return;
                                }
                                inputVars.add(new VariableDTO(var.getType(), var.getNum(), value));
                                inputNumbersIndexes.add(i);
                            }
                        }
                    }
                }

                requiredInputVars.stream()
                        .filter(v -> !inputNumbersIndexes.contains(v.getNum()))
                        .forEach(inputVars::add);


                int diff = inputNumbers.size() - requiredInputVars.size();
                if (diff < 0) {
                    // inputNumbers is shorter -> pad with zeros
                    inputNumbers.addAll(Collections.nCopies(-diff, 0L));
                } else if (diff > 0) {
                    // inputNumbers is longer -> trim
                    inputNumbers = inputNumbers.subList(0, requiredInputVars.size());
                }

                engine.loadInputs(inputVars);
                long result = engine.runProgramAndRecord(degree, inputNumbers);
                System.out.println("Program ran successfully:");
                engine.printProgram(degree);

                System.out.printf("Output: y = %d%n", result);
                System.out.println("Variables:");

                List<List<VariableDTO>> varByType = engine.getVarByType();
                varByType.forEach(list ->
                        list.forEach(var ->
                                System.out.println(var.getName() + " = " + var.getValue())));

                engine.resetVars();

                System.out.printf("Cycles: %d \n", engine.getCycles(degree));
            } else {
                System.out.println("Invalid choice, please try again.");
            }
        }
        else System.out.println("Invalid choice: no program loaded.");
    }
}
