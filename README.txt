# S-Emulator

## Authors:
Anna Ber (211544002) and Sean Berger(318938743)
Emails: 8anna8b6@gmail.com , sean.bkv@gmail.com

## GitHub Repository
https://github.com/seanbrg/S-Emulator


## General System Description
This project implements an **emulator for the S programming language**.
It supports the four basic instructions from S and "synthetic instructions" which can be expanded into a series of basic instructions.
The emulator loads a program described in an XML file, expands synthetic instructions to basic ones by the degree of depth requested, executes the program, and provides a history record of past runs.

The system supports:
- Loading a program from XML.
- Printing the original program.
- Expanding synthetic instructions into basic instructions.
- Running the program with user inputs.
- Managing run history and displaying statistics.


## Engine module Overview

### 'execute' package
- **EngineImpl** - The core class managing program loading, printing, expansion, validation, execution, and history.
- **RunRecord** - Represents a single program run, including run ID, expansion level, inputs, outputs, and cycles.
- **XmlLoader** - Loads a program from XML using DOM and builds the corresponding instructions.

### 'logic.instructions'
- **Instruction (interface)** - The base interface for all instructions (name, label, cycles, execution).
- **AbstractInstruction** - Partial implementation of `Instruction` used by basic instructions.
- **ExpandedInstruction** - Represents an expanded instruction and keeps track of its expansion history.

### 'logic.labels'
- **Label (interface)** - Represents a program label.
- **FixedLabel** - Constant label (e.g., EXIT).
- **NumericLabel** - Numeric labels (L1, L2, …).

### 'logic.program'
- **Program (interface)** - Represents a program structure.
- **SProgram** - Program implementation that manages instructions, labels, execution, validation, and cycle counting.

### 'logic.variables'
- **Variable (interface)** - Represents a general variable with name, type, and value.
- **Var** - Implementation of `Variable` (INPUT, OUTPUT, TEMP).
- **VariableType** - Enum for variable types.

## UI module Overview

### 'console'
 - **ConsoleUI** - Handles the console-based UI of the emulator, allowing to load programs from XML, printing and running them.

### Instructions Design
The instruction system is built around a clear hierarchy. At the core is the 'Instruction' interface, which defines execution, printing, labels, and cycle costs. We implemented **basic instructions** ('Increase', 'Decrease', 'JumpNotZero', 'Neutral') that directly modify variables or control flow as defined in the S language, and **synthetic instructions** ('Assignment', 'ZeroVariable', 'GotoLabel', etc.) to help write complex code that can be compiled into S by the emulator. 
Synthetic instructions are recursively expanded into basic ones with the 'ExpandedInstruction' class keeping track of their history. Instruction metadata such as cycles, type (basic/synthetic), and degree are stored in 'InstructionData'. This design keeps the system modular, allowing us to run high-level programs while tracing their low-level expansion step by step.