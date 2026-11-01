# Tallyne — Architecture Review & Implementation Planning

## I. Your Understanding of Tallyne

Tallyne is an experimental, checklist-inspired esoteric programming language designed around the philosophy of "Programming, one task at a time." The language is deliberately approachable and readable, with its identity deriving from its checklist-like syntax rather than deliberate obfuscation.

### Core Concepts

- **Routines** are named blocks of tasks, the fundamental organizational unit. `{TASKS}` is the default entry point.
- **Tasks** are individual instructions prefixed with `#`, `$`, or `?`, each serving a distinct purpose:
  - `#` Action Tasks: radio, store, return, pull, cut, insert, mark, query, if/else, forever, while
  - `$` Arithmetic Tasks: mathematical expressions with PEMDAS precedence
  - `?` Check Tasks: comparisons that store result in global `call`
- **Program execution** proceeds sequentially from top to bottom within each Routine.
- **Routine declaration** uses `{NAME}` syntax. Routines may accept parameters via `{NAME} with (A), (B)`.
- Three **parameter operations**: `radio` assigns values without execution, `tally` executes with retained parameter values, `prompt` assigns and immediately executes (resetting first).
- **Three Key Variables** shared globally: `it` (general operation results), `yield` (arithmetic results), `call` (check task results).
- **Lists** are ordered collections with zero-based indexing, supporting store, pull, insert-at, insert-into, mark, and cut operations.
- **Entities** (`IO`, sensors, etc.) communicate via `radio` (send information) and `query` (request information, response stored in `it`).

### Intended Product

Tallyne aims to provide two interfaces to the same language implementation:

1. **Local CLI**: Users install Tallyne (initial distribution as Java JAR) and execute `.tly` files via commands like `tallyne run hello.tly`, `tallyne list`, `tallyne unpack package.zip`.
2. **Browser Playground**: Users open a website, write Tallyne code, execute it, and view output without installation.

Both interfaces share the same interpreter and language semantics— they are interfaces to a single implementation, not independent interpreters.

### Language Identity

Tallyne is NOT a general-purpose automation framework, robotics platform, or conventional programming language. It is an esoteric checklist language. The implementation must preserve its checklist-inspired syntax and approachable character. Newlines are significant; indentation is not. String literal spaces must be preserved.

---
## II. Proposed Architecture

### Execution Flow

1. **Source Loading**: Read `.tly` file or from browser editor
2. **Lexical Analysis**: Lexer tokenizes source respecting line boundaries, preserving string literals, distinguishing significant newlines
3. **Line-Sensitive Parsing**: Parser processes tokens line-by-line, constructing AST nodes for Routines, tasks, expressions
4. **AST Construction**: Abstract Syntax Tree representing:
   - Routine declarations with optional parameters
   - Task nodes (Action, Arithmetic, Check)
   - Parameter assignments
   - List literals and operations
   - Conditional and loop constructs
5. **Runtime Execution**: Interpreter traverses AST, maintaining:
   - Execution stack with Routine call contexts
   - Local variable maps per Routine
   - Global Key Variables (`it`, `yield`, `call`)
   - Registered Entity references
6. **Entity Communication**: Interpreter delegates `radio` and `query` operations through the Entity boundary

### Component Communication Flow

```
Source File
    ↓ (Lexer)
Token Stream (line-respecting)
    ↓ (Parser)
AST (Routines → Tasks → Expressions)
    ↓ (Interpreter)
Execution State
    ├── Routine Call Stack
    ├── Local Variable Maps
    ├── Global Key Variables (it, yield, call)
    └── Entity Boundary
        ├── radio() → Routine parameters or Entity handler
        └── query() → Entity response → stored in `it`
```

### Key Architectural Decisions

- **Line-sensitive parsing**: The parser must track logical line boundaries. Whitespace outside string literals is insignificant, but newlines determine task separation. A token-stream-only parser would be unsuitable.
- **Sequential declaration visibility**: Routines become available only after their declaration is encountered (top-to-bottom).
- **Separate local contexts**: Routine calls create new local variable environments; called Routines do not erase caller's locals. Recursive calls maintain separate contexts.
- **Key Variable distinctness**: `it`, `yield`, `call` have separate established purposes and must be preserved as distinct variables.
- **Entity boundary**: Minimal V1 contract allowing registration and communication with Entities through `radio` and `query`.
- **Shared interpreter**: Both CLI and browser playground use the same Java interpreter class/core logic.

---
## III. Proposed Repository Structure

```
tallyne/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/tallyne/
│       │       ├── interpreter/
│       │       │   ├── TallyneInterpreter.java      # Main interpreter entry point
│       │       │   ├── Lexer.java                # Tokenizer, line-sensitive
│       │       │   ├── Parser.java               # Line-sensitive parser
│       │       │   ├── AST.java                  # AST node definitions
│       │       │   ├── Runtime.java              # Execution state, variable maps
│       │       │   ├── EntityBoundary.java       # Minimal V1 Entity contract
│       │       │   └── ExecutionContext.java     # Per-Routine execution state
│       │       ├── cli/
│       │       │   ├── TallyneCLI.java           # CLI entry point
│       │       │   ├── CommandRunner.java       # Command dispatch (run, list, unpack)
│       │       │   └── FileHandler.java         # .tly file reading
│       │       └── playground/
│       │           ├── PlaygroundController.java # Browser endpoint
│       │           └── ExecutionResult.java     # Result formatting for browser
│       │       └── model/
│       │           ├── Routine.java              # Routine AST node
│       │           ├── ActionTask.java           # # task nodes
│       │           ├── ArithmeticTask.java       # $ task nodes
│       │           ├── CheckTask.java            # ? task nodes
│       │           ├── Variable.java             # Variable references
│       │           ├── ListNode.java             # List AST node
│       │           └── Entity.java               # V1 Entity interface
│       │       └── Main.java                   # Application bootstrap
│       └── resources/
│           └── tallyne.jar                     # Runnable JAR manifest
└── docs/
    ├── scheme.md                               # Language specification
    └── README.md                               # User documentation
```

### Directory Responsibilities

- **`src/main/java/com/tallyne/interpreter/`**: Core language implementation - lexer, parser, AST, runtime, interpreter, entity boundary
- **`src/main/java/com/tallyne/cli/`**: Command-line interface, file handling, JAR execution
- **`src/main/java/com/tallyne/playground/`**: Browser-based execution endpoint, HTTP interface
- **`src/main/java/com/tallyne/model/`**: AST node definitions, variable types, Entity contract
- **`docs/`**: Language specification and user documentation (existing files, not to be modified)
- **`src/main/resources/`**: JAR manifest, any resource files needed at runtime

---
## IV. Interpreter Design

### Lexical Analysis

The lexer must be **line-sensitive**, recognizing:

- **Line boundaries**: Each logical line is a separate unit. The lexer must track where one line ends and the next begins.
- **Symbols**: `#`, `$`, `?`, `{`, `}`, `(`, `)`, `//`, `# ***`
- **String literals**: `"value"` - spaces inside must be preserved exactly. The lexer must not strip internal spaces.
- **Identifiers**: Routine names like `{TASKS}`, `{GREETING}` - case-insensitive
- **Numeric literals**: `10`, `5 * 2` within arithmetic
- **Comment handling**: `//` to end of line is ignored
- **Insignificant whitespace**: Spaces outside string literals do not affect meaning, but the lexer must still correctly identify token boundaries

**Critical requirement**: The lexer must preserve string literal contents exactly, including leading/trailing spaces, and must maintain line boundaries so the parser can distinguish separate tasks.

### Line-Sensitive Parsing

The parser must process the token stream with awareness of logical line boundaries:

- Each task occupies its own logical line
- `# ***` terminates the current Routine
- Newlines separate tasks within a Routine
- The parser must not merge tokens across line boundaries in ways that alter meaning

**Parsing sequence**:
1. Read source file
2. Split into logical lines (respecting string literal boundaries)
3. For each line, lexer produces tokens
4. Parser processes tokens line-by-line, constructing appropriate AST nodes

### AST Construction

**Node types**:

- **RoutineNode**: `{NAME}` or `{NAME} with (PARAM1, PARAM2)`; contains list of TaskNode; has boolean isEntryPoint
- **ActionTaskNode**: `#` prefix tasks:
  - `RadioTask`: `# radio (value) to IO` or `# radio (value) to {ROUTINE}`
  - `StoreTask`: `# store (value) in (variable)`
  - `ReturnTask`: `# return (expression)`
  - `PullTask`: `# pull (index) from (list)`
  - `CutTask`: `# cut (index) from (list)`
  - `InsertAtTask`: `# insert (value) at (index) in (list)`
  - `InsertIntoTask`: `# insert (value) into (list)`
  - `MarkTask`: `# mark (value) at (index) in (list)`
  - `QueryTask`: `# query (entity) with (question)`
  - `IfTask`: `# if call is true, ...`
  - `ElseTask`: `# else, ...`
  - `ForeverTask`: `# forever (task)`
  - `WhileTask`: `# while (condition) do (task)`
- **ArithmeticTaskNode**: `$` prefix; contains expression; result stored in `yield`
- **CheckTaskNode**: `?` prefix; contains comparison; result stored in `call`
- **VariableNode**: `(name)` reference; resolves to local or Key Variable
- **ListNode**: `[elements]`; homogeneous element types per spec
- **ParameterNode**: Represents assigned parameter values for `radio`/`prompt`/`tally`

### Routine Registration

- Routines are registered in a case-insensitive map as they are encountered during parsing
- A Routine must be declared before it can be called (sequential visibility)
- Duplicate names produce a declaration error at parse/registration time
- `{TASKS}` is the implicit default entry point, pre-registered

### Routine Execution

- Execution begins at `{TASKS}` Routine
- Tasks execute sequentially within a Routine
- `# ***` terminates the current Routine:
  - In `{TASKS}`, program execution ends
  - In another Routine, control returns to the caller
- `return (value)` places value in `it` but does NOT terminate execution; subsequent tasks continue

### Parameter Handling

- **`radio`**: Assigns positional parameter values to the target Routine without executing it. Repeated `radio` overwrites from first parameter.
- **`tally`**: Executes the target Routine using currently assigned parameter values. Parameter values persist after execution.
- **`prompt`**: Resets parameter values before assigning new ones, then immediately executes. Empty prompt produces error.
- **Missing parameters**: Receive NULL value with context-dependent fallback
- **Extra arguments**: Ignored

### Variable Scopes

- **Local variables**: Belong to their Routine of declaration. Created by `# store (value) in (variable)`. Accessible only within that Routine.
- **Key Variables** (`it`, `yield`, `call`): Shared globally across all Routines. Changes made by one Routine are visible to others.
- **Parameter variables**: Local to the Routine being parameterized; initialized via `radio`/`prompt`/`tally`.

### Key Variables

| Variable | Source | Behavior |
|---|---|---|
| `it` | `return`, `pull`, `cut`, `query` Entity results | General-purpose result holder |
| `yield` | Arithmetic Task `$` expressions | Arithmetic result (stored as string) |
| `call` | Check Task `?` comparisons | Check task result (special Key Variable, not general Boolean) |

**Important**: These are distinct variables with established roles. The interpreter must not conflate them. Whether users can explicitly overwrite them is unresolved (see Unresolved Decisions).

### Arithmetic

- **Syntax**: `$ expression` where expression follows PEMDAS precedence
- **Result storage**: Result stored in `yield` as a **string**
- **NULL handling**: NULL operands treated as zero
- **Numeric string conversion**: Numeric strings converted for arithmetic calculations
- **No separate add/subtract/multiply/divide tasks**: All handled through `$` expression syntax
- **Grammar details unresolved**: Exact modulus notation, division-by-zero behavior, numeric precision

**Implementation approach**: Parse the `$` expression using standard operator precedence. Treat NULL as zero. Convert numeric strings. Store result in `yield` as string.

### Lists

- **Declaration**: `# store ["red","green","blue"] in (colors)`
- **Empty list**: `[]`
- **Zero-based indexing**: First element at index 0
- **Operations**:
  - `pull 0 from (colors)` → retrieves element into `it`
  - `insert "yellow" at 1 in (colors)` → inserts at index
  - `insert "purple" into (colors)` → appends
  - `mark "orange" at 0 in (colors)` → replaces element at index
  - `cut 2 from (colors)` → removes element, stores in `it`
- **Unresolved**: Nested lists permitted element types, invalid index behavior

**Implementation approach**: List is a mutable array-like structure. Operations modify the list in-place. `pull` and `cut` store removed value in `it`.

### Check Tasks and Conditionals

- **Syntax**: `? (operand1) operator (operand2)`
- **Result**: Stored in global `call`
- **Operators**: `=`, `!=`, `is`, `not`, `>`, `<`, `>=`, `<=`, `is empty`
- **Comparison behavior**:
  - `=`, `!=`, `>`, `<`, `>=`, `<=`: Numeric comparison (with string coercion?)
  - `is`: Case-insensitive raw string comparison
  - `not`: Negated raw string comparison
  - `is empty`: Checks if operand is NULL
- **Conditionals**: `# if call is true, (task)` / `# else, (task)`

**Unresolved grammar**: Exact association between check tasks and conditional lines, behavior across multiple conditionals, whether `if`/`else` must appear on immediately following lines.

**Implementation approach**: Parse `?` expression, store result in `call`. For conditionals, look for `# if call is true` and `# else` on subsequent lines. The exact grammar will be minimized for V1.

### Loops

- **`# forever <TASK>`**: Infinite loop
- **`# while (condition) do <TASK>`**: Repeats while condition true

**Unresolved**: Grammar for multi-task loop bodies, nested loops, loop-control operations

**Implementation approach**: V1 supports the two established forms. Multi-task bodies are enclosed within the loop line's scope until `# ***` or another Routine boundary. Nested loops not prioritized for V1.

### Entity Communication

**V1 Minimal Contract**:

The interpreter exposes an `EntityBoundary` interface with two operations:

```java
public interface EntityBoundary {
    // Send information to an Entity (or Routine)
    void radio(String target, String message);
    
    // Request information from an Entity; response stored in `it`
    String query(String entityName, String question);
}
```

**Registration**: The interpreter maintains a reference to an `EntityBoundary` implementation. Entities are registered by name.

**IO Entity**: Pre-registered `IO` entity handles `radio` to output and `query` for input.

**Communication flow**:
- `# radio "Hello" to IO` → `entityBoundary.radio("IO", "Hello")` → output displayed
- `# query IRSensor with "distance"` → `entityBoundary.query("IRSensor", "distance")` → response stored in `it`
- `# query IO` → `entityBoundary.query("IO", "")` → input received, stored in `it`

**V1 vs V2**: V1 only provides the basic boundary. Entity registration is hardcoded or simple callback. V2 will support ZIP-package-loaded third-party Entities.

---
## V. Entity Contract

### V1 Minimal Java Interface

```java
public interface EntityBoundary {
    /**
     * Send information to an Entity or Routine.
     * 
     * @param target The name of the Entity or Routine (e.g., "IO", "WebBrowser")
     * @param message The message/content to send
     * @return true if the radio operation was successful
     */
    boolean radio(String target, String message);
    
    /**
     * Request information from an Entity and store the response in `it`.
     * 
     * @param entityName The name of the Entity to query
     * @param question The question/query to send to the Entity
     * @return The Entity's response string, or null if Entity not found
     */
    String query(String entityName, String question);
}
```

### V1 Entity Registration

The `TallyneInterpreter` maintains an `EntityBoundary` instance:

```java
public class TallyneInterpreter {
    private EntityBoundary entityBoundary;
    
    public void setEntityBoundary(EntityBoundary boundary) {
        this.entityBoundary = boundary;
    }
    
    // In task execution:
    // # radio "Hello World" to IO → entityBoundary.radio("IO", "Hello World")
    // # query IRSensor with "distance" → it = entityBoundary.query("IRSensor", "distance")
}
```

### Pre-registered Entities in V1

- **`IO`**: Pre-registered Alive Entity
  - `radio` to IO produces output (console output for CLI, DOM manipulation for browser)
  - `query IO` requests input from the user/environment, response stored in `it`

### How V1 Communicates

1. **`radio` operation**: Interpreter calls `entityBoundary.radio(target, message)`
   - If target is `"IO"`: output is produced
   - If target is a Routine name: parameters are assigned (Routine parameter radio)
   - For other Entities: custom Entity handler may process

2. **`query` operation**: Interpreter calls `entityBoundary.query(entityName, question)`
   - Response string is placed in the global Key Variable `it`
   - Example: `# query IRSensor with "distance"` → `it` = entity's distance reading

### V1 Deferrals (for V2)

- Third-party Java Entities distributed as ZIP packages
- CLI-based Entity package installation (`tallyne unpack WebBrowser.zip`)
- Programmatic Entity loading via `# load WebBrowser` syntax
- Sophisticated permission/security system
- Entity lifecycle management (alive vs idle distinction formalized)
- Package repository/registry

### V1 Boundary Rationale

The V1 contract is deliberately minimal—a simple interface that allows the interpreter to:
1. Route `radio` messages to named targets
2. Route `query` requests and receive response strings

Everything else (Entity lifecycle, package management, third-party distribution) is deferred to V2. This keeps V1 focused while leaving the integration point extensible.

---
## VI. CLI Design

### Command Structure

```
tallyne <command> [arguments]
```

### Supported Commands

| Command | Description |
|---|---|
| `tallyne run <file.tly>` | Execute a Tallyne source file |
| `tallyne list` | List installed Entities/packages (V1: only IO) |
| `tallyne unpack <package.zip>` | Extract an Entity package (V1: minimal, may be no-op) |

### Execution Flow for `tallyne run hello.tly`

1. CLI reads the `.tly` file content
2. Source passed to `TallyneInterpreter`
3. Interpreter:
   a. Lexer tokenizes respecting line boundaries
   b. Parser constructs AST
   c. Runtime execution begins at `{TASKS}`
   d. Entity boundary interactions (IO, queries)
4. Output captured and displayed in terminal
5. Exit code: 0 for success, non-zero for errors

### JAR Distribution

- Initial distribution as a Java JAR file
- Manifest with `Main-Class: com.tallyne.Main`
- Requires Java 17+ runtime
- Entry point `Main` accepts command-line arguments

### Exit Codes

| Code | Meaning |
|---|---|
| 0 | Successful execution |
| 1 | Lexical error |
| 2 | Syntax error |
| 3 | Runtime error (undefined routine, etc.) |
| 4 | Entity error |

### CLI Code Entry Point

```java
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        switch (command) {
            case "run":
                if (args.length < 2) {
                    error("Usage: tallyne run <file.tly>");
                    System.exit(2);
                }
                runFile(args[1]);
                break;
            case "list":
                listEntities();
                break;
            case "unpack":
                if (args.length < 2) {
                    error("Usage: tallyne unpack <package.zip>");
                    System.exit(2);
                }
                unpackPackage(args[1]);
                break;
            default:
                error("Unknown command: " + command);
                System.exit(1);
        }
    }
}
```

---
## VII. Browser Playground

### Architecture

The browser playground consists of:

1. **Editor**: Text area where users write Tallyne code
2. **Execute Button**: Triggers code execution
3. **Output Area**: Displays program results
4. **Interpreter Backend**: Java (via WebAssembly or GraalVM native) or JavaScript port of the same interpreter

### Interpreter Sharing Between Interfaces

**Critical requirement**: CLI and browser must use the same execution semantics.

**Options**:

1. **Java via WebAssembly**: Compile the Java interpreter to WASM, run in browser. Same JAR logic, same class files.
2. **GraalVM Native Image**: Build a native executable shared between CLI and embedded in web server.
3. **JavaScript Port**: Port the interpreter to JavaScript, ensuring exact same semantics. Must preserve:
   - Line-sensitive parsing
   - Key Variable behavior (`it`, `yield`, `call`)
   - NULL context-dependence
   - Routine call semantics
   - Entity boundary interactions (mock for playground)

**Recommended approach**: Option 1 (Java WASM) or Option 2 (GraalVM) ensures semantic parity. The exact same `.class` files or native image can power both interfaces.

### Execution Flow

1. User writes Tallyne code in the editor
2. Click "Run" sends code to backend via HTTP POST
3. Backend instantiates `TallyneInterpreter`, runs lexer/parser/executor
4. Output captured: `it` values, `yield` results, IO `radio` outputs
5. Results formatted as JSON or HTML and displayed in output area
6. Error information (with line numbers) displayed if execution fails

### Output Display

- **IO radio output**: Displayed as terminal-like output or console messages
- **`it` value**: Shown if modified during execution
- **`yield` value**: Displayed after arithmetic tasks
- **`call` value**: Shown if check task was executed (for conditional display)
- **Error messages**: With source line identification

### Example Playground Session

**Input**:
```tallyne
{TASKS}
# radio "Hello World" to IO
# ***
```

**Output**:
```
Hello World
```

**Input with arithmetic**:
```tallyne
{TASKS}
$ 10 + 5
# radio yield to IO
# ***
```

**Output**:
```
15
```

---
## VIII. Error Handling

### Error Types and Reporting

#### Lexical Errors

- **Unterminated string literal**: Missing closing `"`
- **Invalid symbol**: Unknown character at position
- **Malformed task**: Missing required syntax elements

**Reporting**: Line number and position within line. Example: `Error at line 3: Unterminated string literal`.

#### Syntax Errors

- **Unknown routine name**: Calling a Routine that doesn't exist
- **Missing `# ***`**: Routine without terminator
- **Invalid task for context**: `prompt` without parameters (Empty Prompt error)
- **Parameter mismatches**: Wrong number/type of arguments

**Reporting**: Line number where error detected. Example: `Syntax error at line 7: Unknown routine GREETING`.

#### Undefined Routines

- Calling a Routine that hasn't been declared yet (sequential visibility violation)
- Calling a Routine that was never declared

**Reporting**: Line number of the call site. Example: `Undefined routine at line 12: CALLING_MISSING. Declared at line 3.`

#### Duplicate Declarations

- Two Routines with the same name (case-insensitive)

**Reporting**: Line number of the second/duplicate declaration. Example: `Duplicate routine declaration at line 15: GREETING (already declared at line 3)`.

#### Invalid Operations

- Arithmetic with incompatible types
- List operations with invalid index
- Entity not found

**Reporting**: Line number and context. Example: `Invalid list index at line 5: Index 5 out of range for list of size 3`.

#### Runtime Errors

- Division by zero in arithmetic
- Infinite recursion (detected via call stack limit)
- Entity query failed

**Reporting**: Line number where runtime error occurred. Example: `Runtime error at line 8: Division by zero`.

#### Entity Errors

- Entity not registered
- Entity `radio`/`query` operation failed

**Reporting**: Example: `Entity error at line 3: Entity IO not found. Ensure IO Entity is registered.`

### Error Containment

- Errors should not crash the entire interpreter unless unrecoverable
- Execution stops at the point of error
- Control returns to top-level (CLI: prompt again; Browser: display error in output area)
- Partial output up to the error point may be shown

### Line Identification

All errors must attempt to identify the relevant source line. The lexer/parser must track line numbers during tokenization. When an error is detected at a particular token, the system reports:
- The line number (1-indexed)
- The column position (for precise error pointing)
- A snippet of the erroneous line

---
## IX. Implementation Roadmap

### Phase 0: Foundation (Weeks 1-2)

**What**: Project setup, Java development environment, basic structure

**Components**:
- Initialize Maven/Gradle project
- Create package structure `com.tallyne`
- Write foundational classes: `Main.java`, `TallyneInterpreter.java`
- Copy `scheme.md` and `README.md` as documentation references

**Dependencies**: None beyond JDK

**Test**: Project compiles and runs `java -jar tallyne.jar` with no arguments shows usage

**Completion**: Java project exists, `tallyne run` command syntax recognized

---

### Phase 1: Lexer and Parser (Weeks 3-4)

**What**: Implement line-sensitive lexer and parser

**Components**:
- `Lexer.java`: Tokenizes Tallyne source, respects line boundaries, preserves string literals
- `Parser.java`: Constructs AST from token stream, line-sensitive task parsing
- `AST.java`: RoutineNode, ActionTaskNode, ArithmeticTaskNode, CheckTaskNode, VariableNode, ListNode

**Key challenges**: 
- Line boundary tracking
- String literal space preservation
- Case-insensitive routine name handling
- Distinguishing `# ***` as Routine terminator

**Test**: 
- `tallyne run` on simple programs like the complete example from README
- Verify indentation does not affect execution
- Verify string literals preserve spaces

**Completion**: Parser successfully constructs AST from valid Tallyne source; lexical errors properly detected

---

### Phase 2: Runtime and Execution (Weeks 5-6)

**What**: Implement interpreter runtime, variable management, Routine execution

**Components**:
- `Runtime.java`: Execution state, variable maps, Key Variables (`it`, `yield`, `call`)
- `ExecutionContext.java`: Per-Routine local variable context, parameter handling
- `TallyneInterpreter.java`: Main interpreter entry point, execution orchestration

**Key behaviors to implement**:
- Sequential task execution within Routines
- `# ***` Routine termination (return to caller, program end in `{TASKS}`)
- `return (value)` places in `it` but doesn't terminate
- `radio` assigns parameters without execution
- `tally` executes with retained parameter values
- `prompt` assigns and immediately executes (resets first)
- Local vs Global variable scoping
- Key Variable distinctness

**Test**: 
- Execute the complete example from README producing "The result is: 100"
- Test Routine calls and recursion
- Test parameter handling (radio, tally, prompt)
- Test Key Variable behavior

**Completion**: Basic Tallyne programs execute correctly; variable scoping works as specified

---

### Phase 3: Lists and Arithmetic (Weeks 7-8)

**What**: Implement list operations and arithmetic task evaluation

**Components**:
- ListNode and associated operations (pull, insert-at, insert-into, mark, cut)
- ArithmeticTaskNode evaluation with PEMDAS
- NULL handling in arithmetic (treated as zero)

**Key behaviors**:
- `$` prefix arithmetic with result in `yield` as string
- List creation: `# store ["a","b"] in (colors)`
- Zero-based list indexing
- `pull`, `cut` → store result in `it`
- `insert at`, `insert into`, `mark`
- NULL as zero in arithmetic

**Test**:
- Arithmetic: `$ 10 + 5 * 2` → `yield` = "20" (PEMDAS: 10 + (5*2) = 20)
- Lists: create, pull, insert, mark, cut
- Mixed arithmetic and list operations

**Completion**: Lists and arithmetic integrate with the runtime; NULL behavior consistent

---

### Phase 4: Check Tasks, Conditionals, and Loops (Weeks 9-10)

**What**: Implement check tasks, conditional logic, and loop structures

**Components**:
- CheckTaskNode with comparison operators
- Conditional tasks: `# if call is true, ...` / `# else, ...`
- Loop forms: `# forever <task>` and `# while (condition) do <task>`

**Key behaviors**:
- `?` prefix stores result in `call`
- Numeric equality `=` and inequality `!=`
- Case-insensitive `is` and negated `not`
- `is empty` NULL check
- Conditional execution based on `call`
- Infinite `# forever` loop
- `# while (condition) do task` loop

**Test**:
- Check tasks with various operators
- Conditionals: `if call is true` / `else`
- Forever loop (with safe timeout for testing)
- While loop with terminating condition

**Completion**: Conditionals and loops execute as specified; `call` variable properly propagated

---

### Phase 5: Entity Boundary and IO (Weeks 11-12)

**What**: Implement minimal Entity contract, IO integration

**Components**:
- `EntityBoundary.java`: Minimal Java interface
- `IO Entity`: Pre-registered handler for radio/query
- Entity integration into interpreter

**Key behaviors**:
- `# radio "Hello" to IO` → output to console
- `# query IO` → input received, stored in `it`
- `# query IRSensor with "distance"` → Entity response in `it`
- Entity not found error

**Test**:
- Programs using IO radio output
- Programs using query IO for input
- Entity error handling when Entity not registered

**Completion**: Entity boundary functional; IO integrates with CLI output and browser display

---

### Phase 6: CLI and Browser Playground (Weeks 13-14)

**What**: Implement CLI interface and browser playground integration

**Components**:
- CLI: `tallyne run`, `tallyne list`, `tallyne unpack`
- Browser: Execution endpoint, output formatting
- Shared interpreter core between both interfaces

**Key behaviors**:
- CLI command dispatch and file handling
- Browser HTTP execution endpoint
- Output capture and display
- Error reporting with line numbers

**Test**:
- `tallyne run hello.tly` executes file successfully
- Browser playground writes and runs code, shows output
- Both interfaces produce identical results for same code

**Completion**: Both CLI and playground functional with shared semantics

---

### Phase 7: Polishing and Roadmap Completion (Weeks 15-16)

**What**: Error handling, edge cases, documentation, final testing

**Components**:
- Complete error handling for all error types
- Edge case testing (empty programs, NULL values, etc.)
- Exit code specification
- Browser polish

**Test**: Full test suite covering all language features

**Completion**: Tallyne V1 implementation feature-complete per specification

---
## X. Risks and Unresolved Decisions

### Ambiguities in the Specification

| Area | Unresolved Question | Architectural Implication |
|---|---|---|
| **NULL value model** | What is the intended context-dependent value model? How does NULL interact with different operations? | The interpreter must handle NULL flexibly without imposing a conventional type system. NULL-as-zero in arithmetic is established, but other interactions are open. |
| **Key Variable overwrite** | Can users explicitly overwrite `it`, `yield`, and `call`? | This affects variable assignment semantics. Must clarify before finalizing variable storage model. |
| **Arithmetic modulus** | What is the exact modulus notation? | Parser must support or reject modulus operator. If unsupported, must document limitation. |
| **Arithmetic division-by-zero** | How are division by zero and numeric precision handled? | Must define behavior (error? infinity? NULL?) before implementing arithmetic evaluator. |
| **List nested structures** | What values and nested structures are permitted in lists? | Affects ListNode design. V1 may restrict to homogeneous simple types. |
| **List invalid index behavior** | What happens when an index is invalid? | Must define: error, NULL in `it`, or wrap-around. |
| **`if`/`else` grammar** | What is the exact `if`/`else` grammar and association? | Affects parser structure. May be line-dependent or brace-dependent. |
| **Multi-task loop bodies** | How are multi-task loop bodies represented? | Loop body scope must be defined. May extend until `# ***` or Routine boundary. |
| **Nested loop support** | Are nested loops supported, and how are they controlled? | V1 may restrict to single-level loops only. |
| **NULL fallback for missing parameters** | What are the exact NULL fallback rules for missing arguments? | Affects `radio`/`prompt`/`tally` parameter handling. |
| **Minimum formal Java Entity contract** | What is the minimum formal Java Entity contract? | Determines `EntityBoundary` interface complexity. V1 keeps this minimal. |
| **Empty lines and multi-line statements** | Are empty lines ignored, and can any statement span multiple lines? | Affects lexer/parser line-splitting logic. |
| **User overwrite of Key Variables** | Can users explicitly overwrite `it`, `yield`, and `call`? | Affects whether assignment tasks like `# store (value) in it` are allowed. |

### Architectural Risks

1. **Line-sensitive parsing complexity**: Implementing a parser that respects line boundaries while ignoring insignificant whitespace is non-trivial. Must be carefully designed to avoid bugs.

2. **Shared interpreter between CLI and browser**: Ensuring semantic parity between Java (CLI) and JavaScript/WASM (browser) implementations is critical. Any divergence breaks the "shared semantics" requirement.

3. **NULL handling**: The context-dependent NULL model is the most significant unspecified area. Poor handling could break multiple features (arithmetic, lists, conditionals).

4. **Entity boundary extensibility**: Designing a V1 contract that is minimal yet leaves room for V2 package ecosystem requires careful balance.

5. **Conditional and conditional grammar**: The `if`/`else` association and check task conditional grammar are unresolved. Over-specifying early may lock in a suboptimal design.

### Decisions Requiring Approval

1. **Arithmetic precision and division-by-zero**: Should division by zero produce an error, result in NULL, or some other value?

2. **NULL interaction model**: Beyond arithmetic (where NULL = 0), how should NULL behave in other contexts (list operations, conditionals, variable assignment)?

3. **Key Variable overwrite policy**: Should `it`, `yield`, `call` be explicitly overwritable by user programs, or are they write-once (receiving values from operations only)?

4. **List element types and nesting**: Should V1 restrict lists to homogeneous simple types, or permit nested lists and mixed types?

5. **Conditional grammar formalization**: Should `# if call is true` / `# else` be restricted to immediately following lines, or can they appear with separation?

6. **Loop body scope**: For `# forever task` and `# while (cond) do task`, does the body extend to the next `# ***` / Routine boundary, or must it be a single task?

7. **Entity V1 scope**: Should V1 include only `IO` as a pre-registered Entity, or should the `EntityBoundary` interface be open for any Entity implementation?

8. **Browser implementation technology**: Java WASM, GraalVM native image, or JavaScript port— which approach best balances semantic parity with practical deployability?

9. **Error recovery level**: Should the interpreter halt at the first error, or attempt to continue and report multiple errors?

10. **Package naming and case sensitivity**: Routine names are case-insensitive. Should Entity names follow the same rule?

---