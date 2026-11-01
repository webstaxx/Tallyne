package com.tallyne.interpreter;

import com.tallyne.model.*;

import java.io.*;
import java.util.*;

/**
 * TallyneInterpreter - The main interpreter class.
 * 
 * Responsibilities:
 * - Load and tokenize Tallyne source (.tly files)
 * - Parse token stream into AST (RoutineNode trees)
 * - Execute the AST via the Runtime
 * - Manage global state (Key Variables it, yield, call)
 * - Manage Routine registry (case-insensitive)
 * - Handle Entity boundary communication
 * - Provide CLI and future browser integration entry points
 * 
 * The interpreter is designed to be independent of the CLI layer,
 * so other execution interfaces (browser, etc.) can use the same core.
 */
public class TallyneInterpreter {
    
    // Routine registry: case-insensitive map of routine name -> RoutineNode
    private final Map<String, RoutineNode> routineRegistry;
    
    // Entity boundary for Entity communication
    private EntityBoundary entityBoundary;
    
    // Output consumer - will be System.out for CLI, or a consumer for browser
    private java.util.Consumer<String> outputConsumer;
    
    // Error consumer
    private java.util.Consumer<String> errorConsumer;
    
    // Track lexical and syntax errors
    private int lexicalErrors;
    private int syntaxErrors;
    
    // Track the last error line for reporting
    private int lastErrorLine;
    
    // BigDecimal precision for arithmetic
    private static final int ARITHMETIC_PRECISION = 10;
    
    public TallyneInterpreter() {
        this.routineRegistry = new HashMap<>();
        this.lastErrorLine = 0;
        this.lexicalErrors = 0;
        this.syntaxErrors = 0;
        
        // Default output consumer: print to stdout
        this.outputConsumer = System.out::println;
        this.errorConsumer = System.err::println;
        
        // Pre-register the IO Entity
        preRegisterIOEntity();
    }
    
    /**
     * Pre-register the IO Entity for V1.
     * IO handles radio output and query input.
     */
    private void preRegisterIOEntity() {
        // The IO Entity is handled through the EntityBoundary interface
        // In the CLI implementation, IO radio produces console output
        // IO query requests user input
        // For now, we set up a default entity boundary that handles IO
        setEntityBoundary(new EntityBoundary() {
            @Override
            public boolean radio(String target, String message) {
                if (target.equalsIgnoreCase("IO")) {
                    acceptOutput(message);
                    return true;
                }
                // Radio to other entities or routines handled elsewhere
                return false;
            }
            
            @Override
            public String query(String entityName, String question) {
                if (entityName.equalsIgnoreCase("IO")) {
                    // For CLI: request input from user
                    // For now, return null (will be handled by CLI input logic)
                    return null;
                }
                return null;
            }
        });
    }
    
    /**
     * Set the Entity boundary for the interpreter.
     */
    public void setEntityBoundary(EntityBoundary boundary) {
        this.entityBoundary = boundary;
    }
    
    /**
     * Get the Entity boundary.
     */
    public EntityBoundary getEntityBoundary() {
        return entityBoundary;
    }
    
    /**
     * Get a routine from the registry (case-insensitive lookup).
     */
    public RoutineNode getRoutine(String name) {
        if (name == null) {
            return null;
        }
        return routineRegistry.get(name.toLowerCase());
    }
    
    /**
     * Register a routine in the registry.
     * Duplicate names (case-insensitive) will overwrite with a warning.
     */
    public void registerRoutine(RoutineNode routine) {
        if (routine != null && routine.getName() != null) {
            String lowerName = routine.getName().toLowerCase();
            if (routineRegistry.containsKey(lowerName)) {
                // Duplicate - warn but allow overwrite for now
                error("Warning: Duplicate routine declaration (case-insensitive): " + routine.getName());
            }
            routineRegistry.put(lowerName, routine);
        }
    }
    
    /**
     * Accept output through the consumer (CLI prints, browser displays).
     */
    private void acceptOutput(String message) {
        if (outputConsumer != null) {
            outputConsumer.accept(message);
        }
    }
    
    /**
     * Report an error message through the error consumer.
     * Includes line number if available.
     */
    public void error(String message) {
        lastErrorLine = getLastErrorLine();
        errorConsumer.accept(message);
        syntaxErrors++;
    }
    
    /**
     * Get the last error line number.
     */
    public int getLastErrorLine() {
        return lastErrorLine;
    }
    
    /**
     * Get the number of lexical errors.
     */
    public int getLexicalErrors() {
        return lexicalErrors;
    }
    
    /**
     * Get the number of syntax errors.
     */
    public int getSyntaxErrors() {
        return syntaxErrors;
    }
    
    /**
     * Reset error counters.
     */
    public void resetErrors() {
        lexicalErrors = 0;
        syntaxErrors = 0;
        lastErrorLine = 0;
    }
    
    /**
     * Execute a Tallyne program from source string.
     * 
     * @source The Tallyne source code as a string
     * @return true if execution completed successfully, false on error
     */
    public boolean execute(String source) {
        resetErrors();
        
        if (source == null || source.trim().isEmpty()) {
            error("Empty program");
            return false;
        }
        
        try {
            // Step 1: Lexical analysis
            Lexer lexer = new Lexer(source);
            List<Lexer.Token> tokens = lexer.tokenize();
            
            // Step 2: Parse the token stream into AST
            Parser parser = new Parser(tokens);
            List<RoutineNode> routines = parser.parse();
            
            if (routines.isEmpty()) {
                error("No routines found in program");
                return false;
            }
            
            // Step 3: Register all routines
            for (RoutineNode routine : routines) {
                if (routine.getName() != null) {
                    registerRoutine(routine);
                }
            }
            
            // Step 4: Execute the program
            // Execution begins from the {TASKS} routine
            RoutineNode tasksRoutine = getRoutine("tasks");
            if (tasksRoutine == null) {
                // Try {TASKS} case-insensitively
                tasksRoutine = getRoutine("TASKS");
            }
            
            if (tasksRoutine == null) {
                error("No {TASKS} routine found - program has no entry point");
                return false;
            }
            
            // Create runtime context and execute
            RuntimeContext context = new RuntimeContext(this, new Runtime(this, null));
            context.setCurrentRoutine(tasksRoutine);
            
            // Execute the tasks in {TASKS}
            boolean success = executeRoutineTasks(tasksRoutine, context);
            
            if (success) {
                // Program completed normally
                // Check if {TASKS} terminated via # *** (program end)
                // In {TASKS}, # *** terminates the program
            }
            
            return success;
            
        } catch (Exception e) {
            error("Runtime error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute the tasks within a routine.
     */
    private boolean executeRoutineTasks(RoutineNode routine, RuntimeContext context) {
        if (routine == null || routine.getTasks() == null) {
            return true;
        }
        
        for (TaskNode task : routine.getTasks()) {
            if (task == null) {
                continue;
            }
            
            // Check for specific task types
            if (task instanceof RadioTaskNode) {
                RadioTaskNode radioTask = (RadioTaskNode) task;
                boolean cont = radioTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof StoreTaskNode) {
                StoreTaskNode storeTask = (StoreTaskNode) task;
                boolean cont = storeTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof ReturnTaskNode) {
                ReturnTaskNode returnTask = (ReturnTaskNode) task;
                // executeReturn places in it but does NOT terminate
                boolean cont = returnTask.execute(context);
                if (!cont) {
                    return false;
                }
                // Important: execution continues after return
            } else if (task instanceof PullTaskNode) {
                PullTaskNode pullTask = (PullTaskNode) task;
                boolean cont = pullTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof CutTaskNode) {
                CutTaskNode cutTask = (CutTaskNode) task;
                boolean cont = cutTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof InsertTaskNode) {
                InsertTaskNode insertTask = (InsertTaskNode) task;
                boolean cont = insertTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof MarkTaskNode) {
                MarkTaskNode markTask = (MarkTaskNode) task;
                boolean cont = markTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof QueryTaskNode) {
                QueryTaskNode queryTask = (QueryTaskNode) task;
                boolean cont = queryTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof CheckTaskNode) {
                CheckTaskNode checkTask = (CheckTaskNode) task;
                boolean cont = checkTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof IfTaskNode) {
                IfTaskNode ifTask = (IfTaskNode) task;
                boolean cont = ifTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof ElseTaskNode) {
                ElseTaskNode elseTask = (ElseTaskNode) task;
                boolean cont = elseTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof ForeverTaskNode) {
                ForeverTaskNode foreverTask = (ForeverTaskNode) task;
                boolean cont = foreverTask.execute(context);
                if (!cont) {
                    return false;
                }
                // Note: # forever creates an infinite loop;
                // the interpreter will have a stack depth limit
            } else if (task instanceof WhileTaskNode) {
                WhileTaskNode whileTask = (WhileTaskNode) task;
                boolean cont = whileTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof RoutineCallTaskNode) {
                RoutineCallTaskNode callTask = (RoutineCallTaskNode) task;
                boolean cont = callTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else if (task instanceof ArithmeticTaskNode) {
                ArithmeticTaskNode arithmeticTask = (ArithmeticTaskNode) task;
                boolean cont = arithmeticTask.execute(context);
                if (!cont) {
                    return false;
                }
            } else {
                // Unknown task type - skip
                interpreter.output("Warning: Unknown task type encountered");
            }
        }
        
        return true;
    }
    
    /**
     * Load a .tly file and execute it.
     */
    public boolean loadAndRunFile(String filePath) {
        StringBuilder source = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            error("File not found: " + filePath);
            return false;
        } catch (IOException e) {
            error("Error reading file: " + e.getMessage());
            return false;
        }
        
        return execute(source.toString());
    }
    
    /**
     * Main method - entry point for the CLI.
     * 
     * Usage: java -jar tallyne.jar run <file.tly>
     *        java -jar tallyne.jar list
     *        java -jar tallyne.jar unpack <package.zip>
     */
    public static void main(String[] args) {
        TallyneInterpreter interpreter = new TallyneInterpreter();
        
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "run":
                if (args.length < 2) {
                    error("Usage: tallyne run <file.tly>");
                    System.exit(2);
                }
                String filePath = args[1];
                boolean ran = interpreter.loadAndRunFile(filePath);
                if (!ran) {
                    System.exit(3);
                }
                break;
            case "list":
                listRoutines();
                break;
            case "unpack":
                if (args.length < 2) {
                    error("Usage: tallyne unpack <package.zip>");
                    System.exit(2);
                }
                String zipPath = args[1];
                // V1: unpack is a no-op/placeholder
                info("Unpack: V1 does not support package installation. " +
                      "This is a V2 feature. " +
                      "File: " + zipPath + " was acknowledged but not processed.");
                break;
            default:
                error("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }
    
    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("Tallyne V1 - Esoteric checklist programming language");
        System.out.println("Usage:");
        System.out.println("  tallyne run <file.tly>   Execute a Tallyne source file");
        System.out.println("  tallyne list           List registered routines");
        System.out.println("  tallyne unpack <zip>   Acknowledge a package (V1 placeholder)");
    }
    
    /**
     * List registered routines.
     */
    private static void listRoutines() {
        System.out.println("Tallyne Routines:");
        // In a real implementation, we'd list from the interpreter's registry
        System.out.println("  (no routines registered in V1 placeholder)");
    }
    
    /**
     * Output an info message.
     */
    private static void info(String message) {
        System.out.println(message);
    }
}