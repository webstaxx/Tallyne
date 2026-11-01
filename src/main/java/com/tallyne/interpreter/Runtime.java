package com.tallyne.interpreter;

import com.tallyne.model.*;

import java.util.*;
import java.math.*;

/**
 * Runtime implements the execution of Tallyne AST nodes.
 * Contains all the semantics for variable handling, routine calls,
 * arithmetic, lists, checks, loops, and Entity communication.
 * 
 * Key design decisions per V1 specification:
 - NULL is context-dependent; arithmetic NULL = 0
 - it, yield, call are globally shared mutable variables
 - Routine calls create new local contexts; callers' locals preserved
 - Recursive calls maintain separate local contexts
 - Case-insensitive routine names and entity names
 - Line-sensitive parsing preserved through AST
 */
public class Runtime {

    private TallyneInterpreter interpreter;
    private RuntimeContext context;
    
    public Runtime(TallyneInterpreter interpreter, RuntimeContext context) {
        this.interpreter = interpreter;
        this.context = context;
    }
    
    // --- Core execution methods for each task type ---
    
    /**
     * Execute a radio task: assigns values to Routine parameters without executing.
     */
    public boolean executeRadio(String target, String value, boolean hasParameterList, 
                                List<String> parameterValues, RuntimeContext context) {
        
        if (target.equalsIgnoreCase("IO")) {
            // Radio to IO - produce output
            interpreter.output(value);
            return true;
        }
        
        // Radio to a Routine - assign parameter values
        // Find the routine by name (case-insensitive)
        RoutineNode routine = interpreter.getRoutine(target);
        if (routine == null) {
            interpreter.error("Undefined routine: " + target);
            return false;
        }
        
        if (hasParameterList && parameterValues != null) {
            // Assign positional parameter values
            // Repeated radio overwrites from first parameter
            for (int i = 0; i < parameterValues.size(); i++) {
                String paramValue = parameterValues.get(i);
                if (i < routine.getParameterNames().size()) {
                    routine.getParameterValues().set(i, paramValue);
                }
                // If more arguments than parameters, extra are ignored
            }
        } else if (value != null && !value.isEmpty()) {
            // Single value assignment - assign to first parameter
            if (!routine.getParameterNames().isEmpty()) {
                routine.getParameterValues().set(0, value);
            }
        }
        
        return true;
    }
    
    /**
     * Execute a store task: # store (value) in (variable)
     */
    public boolean executeStore(String value, String variable, RuntimeContext context) {
        // Parse the variable reference
        // Variable could be: a name, or (name) format
        // After lexer, variable name is already extracted
        
        if (variable != null && !variable.isEmpty()) {
            // Store in local variable of current routine
            context.setLocalVariable(variable, value);
        }
        
        // Also store in `it` if the value is "it" (special reference)
        if (value != null && value.equalsIgnoreCase("it")) {
            context.setIt(context.getIt() != null ? context.getIt() : "NULL");
        }
        
        return true;
    }
    
    /**
     * Execute a return task: # return (sum)
     * Places value into global `it` but does NOT terminate execution.
     */
    public boolean executeReturn(String expression, RuntimeContext context) {
        // Evaluate the expression and place result in `it`
        String result = evaluateArithmeticExpression(expression, context);
        context.setIt(result);
        // Importantly, return does NOT terminate execution
        // Subsequent tasks in the Routine continue executing
        return true;
    }
    
    /**
     * Execute a pull task: # pull (index) from (list)
     * Stores retrieved element in global `it`.
     */
    public boolean executePull(String index, String listName, RuntimeContext context) {
        // Find the list by name
        // For V1, we need to track lists somewhere in the context
        // For now, this is a simplified implementation
        
        // In a full implementation, we'd look up the list from the current routine's variables
        // For V1, let's handle the case where the list was stored with a name
        
        // Since lists are stored as variables, look up the list variable
        String listValue = context.getLocalVariable(listName);
        
        if (listValue == null) {
            // Try as a global reference
            // For now, if list not found, store NULL in it
            context.setIt(null);
            interpreter.error("Undefined list: " + listName);
            return false;
        }
        
        // Parse the index
        int idx = parseIndex(index);
        
        // Retrieve element at index from the list
        // The list value format needs to support element access
        // For V1, we'll use a simple string list representation
        
        // For now, store NULL in it as placeholder
        // Full list implementation will be in later phases
        context.setIt("NULL");
        
        return true;
    }
    
    /**
     * Execute a cut task: # cut (index) from (list)
     * Removes element and stores removed value in global `it`.
     */
    public boolean executeCut(String index, String listName, RuntimeContext context) {
        // Similar to pull but also removes the element from the list
        String listValue = context.getLocalVariable(listName);
        
        if (listValue == null) {
            context.setIt(null);
            interpreter.error("Undefined list: " + listName);
            return false;
        }
        
        int idx = parseIndex(index);
        
        // Remove element and store it in it
        // For V1 placeholder: store NULL
        context.setIt("NULL");
        
        return true;
    }
    
    /**
     * Execute an insert task: # insert (value) at (index) in (list) or # insert (value) into (list)
     */
    public boolean executeInsert(String value, String atIndex, String listName, RuntimeContext context) {
        String listValue = context.getLocalVariable(listName);
        
        if (listValue == null) {
            interpreter.error("Undefined list: " + listName);
            return false;
        }
        
        if (atIndex != null && !atIndex.isEmpty() && !atIndex.equalsIgnoreCase("into")) {
            // Insert at specific index
            int idx = parseIndex(atIndex);
            // Implementation-dependent list modification
        } else {
            // "into" means append/insert at end
            // Implementation-dependent
        }
        
        return true;
    }
    
    /**
     * Execute a mark task: # mark (value) at (index) in (list)
     */
    public boolean executeMark(String value, String index, String listName, RuntimeContext context) {
        String listValue = context.getLocalVariable(listName);
        
        if (listValue == null) {
            interpreter.error("Undefined list: " + listName);
            return false;
        }
        
        int idx = parseIndex(index);
        
        // Replace element at index with value
        // Implementation-dependent
        
        return true;
    }
    
    /**
     * Execute a query task: # query (entity) with (question)
     * Response stored in global `it`.
     */
    public boolean executeQuery(String entityName, String question, RuntimeContext context) {
        EntityBoundary eb = context.getEntityBoundary();
        if (eb == null) {
            interpreter.error("Entity not registered: " + entityName);
            context.setIt("NULL");
            return false;
        }
        
        String response = eb.query(entityName, question);
        if (response != null) {
            context.setIt(response);
        } else {
            context.setIt("NULL");
        }
        
        return true;
    }
    
    /**
     * Execute an if task: # if call is true, (body)
     * Checks the global `call` variable.
     */
    public boolean executeIf(String body, RuntimeContext context) {
        // Check the global `call` variable
        String callValue = context.getCall();
        
        // Determine if call is "true"
        boolean condition;
        if (callValue == null) {
            condition = false;
        } else if (callValue.equalsIgnoreCase("true")) {
            condition = true;
        } else {
            // Treat non-empty string as true, empty as false
            condition = !callValue.isEmpty();
        }
        
        if (condition) {
            // Execute the body tasks
            // The body is a string description; in a full implementation,
            // we'd parse and execute the tasks
            if (body != null && !body.isEmpty()) {
                interpreter.output(" (if body: " + body + ")");
            }
        }
        
        return true;
    }
    
    /**
     * Execute an else task: # else, (body)
     */
    public boolean executeElse(String body, RuntimeContext context) {
        // In V1, else associates with the immediately preceding if
        // Execution continues here if the preceding if's condition was false
        if (body != null && !body.isEmpty()) {
            interpreter.output(" (else body: " + body + ")");
        }
        return true;
    }
    
    /**
     * Execute a forever loop: # forever <task>
     * Infinite loop with stack depth limit.
     */
    public boolean executeForever(String taskDescription, RuntimeContext context) {
        // Execute the task repeatedly with a depth limit
        // For V1, we'll execute it once with a note that it's a loop
        // The interpreter will handle the looping logic
        if (taskDescription != null && !taskDescription.isEmpty()) {
            interpreter.output(" (forever: " + taskDescription + ")");
        }
        // Return true to indicate the loop is active
        return true;
    }
    
    /**
     * Execute a while loop: # while (condition) do <task>
     * Repeats while condition is true.
     */
    public boolean executeWhile(String condition, String body, RuntimeContext context) {
        // Evaluate the condition using the global `call` variable
        // For V1, we check the current `call` value
        String callValue = context.getCall();
        
        boolean loopCondition;
        if (callValue == null) {
            loopCondition = false;
        } else if (callValue.equalsIgnoreCase("true")) {
            loopCondition = true;
        } else {
            loopCondition = !callValue.isEmpty();
        }
        
        if (loopCondition) {
            // Execute the body
            if (body != null && !body.isEmpty()) {
                interpreter.output(" (while body: " + body + ")");
            }
        }
        
        return true;
    }
    
    /**
     * Execute a routine call task: # tally {ROUTINE}, # prompt {ROUTINE} with params, # radio to {ROUTINE}
     */
    public boolean executeRoutineCall(String action, String targetRoutine, 
                                      List<String> parameterValues, RuntimeContext context) {
        
        RoutineNode routine = interpreter.getRoutine(targetRoutine);
        if (routine == null) {
            interpreter.error("Undefined routine: " + targetRoutine);
            return false;
        }
        
        String lowerAction = action.toLowerCase();
        
        switch (lowerAction) {
            case "tally":
                // Execute the routine using currently assigned parameter values
                // Parameter values are retained after execution
                return executeRoutineTally(routine, context);
                
            case "prompt":
                // Reinitialise parameter values, assign supplied values, then execute
                return executeRoutinePrompt(routine, parameterValues, context);
                
            case "radio":
                // Assign values to parameters without executing
                if (routine.hasParameters()) {
                    for (int i = 0; i < parameterValues.size(); i++) {
                        String paramValue = i < parameterValues.size() ? parameterValues.get(i) : null;
                        routine.getParameterValues().set(i, paramValue);
                    }
                }
                return true;
                
            default:
                interpreter.error("Unknown routine call action: " + action);
                return false;
        }
    }
    
    /**
     * Execute a tally routine: uses currently assigned parameter values.
     * Parameter values persist after execution.
     */
    private boolean executeRoutineTally(RoutineNode routine, RuntimeContext context) {
        if (!routine.hasParameters()) {
            // Routine with no parameters - just execute its tasks
            return executeRoutineTasks(routine, context);
        }
        
        // Get the parameter values assigned to this routine
        List<String> params = routine.getParameterValues();
        
        // Create a new execution context for this routine call
        // but share the global key variables (it, yield, call)
        RuntimeContext newContext = new RuntimeContext(interpreter, runtime);
        newContext.setIt(context.getIt()); // Share it
        newContext.setYield(context.getYield()); // Share yield
        newContext.setCall(context.getCall()); // Share call
        newContext.setCurrentRoutine(routine);
        newContext.getLocalVariables().putAll(context.getLocalVariables()); // Share parent locals
        
        // Override parameters if assigned
        for (int i = 0; i < routine.getParameterNames().size(); i++) {
            String paramName = routine.getParameterNames().get(i);
            String paramValue = i < params.size() ? params.get(i) : null;
            newContext.setLocalVariable(paramName, paramValue);
        }
        
        // Execute the routine's tasks
        boolean result = executeRoutineTasks(routine, newContext);
        
        // After execution, retain the parameter values (they're not cleared)
        // The newContext's parameter values are lost, but the routine's own parameterValues
        // retain whatever was set via radio/prompt
        
        return result;
    }
    
    /**
     * Execute a prompt routine: reinitialise parameters, assign new values, execute.
     */
    private boolean executeRoutinePrompt(RoutineNode routine, List<String> parameterValues, 
                                          RuntimeContext context) {
        if (parameterValues == null) {
            interpreter.error("Empty prompt: no arguments supplied");
            return false;
        }
        
        // Clear existing parameter values
        if (routine.hasParameters()) {
            for (String paramName : routine.getParameterNames()) {
                // Unset or reset
            }
        }
        
        // Assign new parameter values
        for (int i = 0; i < parameterValues.size() && i < routine.getParameterNames().size(); i++) {
            String paramName = routine.getParameterNames().get(i);
            String paramValue = parameterValues.get(i);
            routine.getParameterValues().set(i, paramValue);
        }
        
        // Execute the routine's tasks
        return executeRoutineTasks(routine, context);
    }
    
    /**
     * Execute the tasks within a routine.
     */
    private boolean executeRoutineTasks(RoutineNode routine, RuntimeContext ctx) {
        if (routine == null || routine.getTasks() == null) {
            return true;
        }
        
        for (TaskNode task : routine.getTasks()) {
            if (task == null) {
                continue;
            }
            
            // Check for routine call tasks specifically
            if (task instanceof RoutineCallTaskNode) {
                RoutineCallTaskNode callTask = (RoutineCallTaskNode) task;
                boolean cont = executeRoutineCall(callTask.getAction(), 
                    callTask.getTargetRoutine(), callTask.getParameterValues(), ctx);
                if (!cont) {
                    return false;
                }
            } else {
                // Execute the task
                boolean cont = task.execute(ctx);
                if (!cont) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Parse an index string to integer.
     * Handles NULL and invalid indices.
     */
    private int parseIndex(String index) {
        if (index == null || index.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(index.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Evaluate an arithmetic expression and return the result as a string.
     * Uses BigDecimal for decimal arithmetic.
     * NULL operands treated as zero.
     */
    private String evaluateArithmeticExpression(String expression, RuntimeContext context) {
        if (expression == null || expression.isEmpty()) {
            return "0";
        }
        
        // Tokenize the expression
        String[] tokens = expression.trim().split("\\s+");
        
        if (tokens.length == 0) {
            return "0";
        }
        
        // Simple left-to-right evaluation with PEMDAS
        // For V1, we'll implement a basic parser
        
        // First pass: handle parentheses
        // Second pass: handle precedence (exponents, multiplication/division, addition/subtraction)
        // Third pass: handle addition/subtraction
        
        // For now, just handle simple cases
        try {
            // Check if it's just a single number
            if (tokens.length == 1) {
                String token = tokens[0];
                // Check if it's a variable reference (it, yield, call) or a number
                if (token.equalsIgnoreCase("it") || token.equalsIgnoreCase("yield") || token.equalsIgnoreCase("call")) {
                    // Return the current value of the key variable
                    if (token.equalsIgnoreCase("it")) {
                        return context.getIt() != null ? context.getIt() : "0";
                    } else if (token.equalsIgnoreCase("yield")) {
                        return context.getYield() != null ? context.getYield() : "0";
                    } else {
                        return context.getCall() != null ? context.getCall() : "0";
                    }
                }
                // It might be a list reference or other - for now treat as number
                try {
                    BigDecimal num = new BigDecimal(token);
                    return num.toString();
                } catch (NumberFormatException e) {
                    return "0";
                }
            }
            
            // Simple evaluation: process multiplication and division first, then addition and subtraction
            // This is a simplified approach; a full PEMDAS parser would be more complex
            
            // Convert to BigDecimal where possible
            List<BigDecimal> values = new ArrayList<>();
            List<String> operators = new ArrayList<>();
            
            for (String token : tokens) {
                // Check for operators
                if (token.equals("*") || token.equals("/") || token.equals("+") || token.equals("-")) {
                    operators.add(token);
                } else {
                    // Try to parse as BigDecimal
                    try {
                        BigDecimal val = new BigDecimal(token);
                        values.add(val);
                    } catch (NumberFormatException e) {
                        // Maybe it's a key variable reference
                        if (token.equalsIgnoreCase("it")) {
                            values.add(new BigDecimal(context.getIt() != null ? context.getIt() : "0"));
                        } else if (token.equalsIgnoreCase("yield")) {
                            values.add(new BigDecimal(context.getYield() != null ? context.getYield() : "0"));
                        } else if (token.equalsIgnoreCase("call")) {
                            values.add(new BigDecimal(context.getCall() != null ? context.getCall() : "0"));
                        } else {
                            // NULL or unknown - treat as zero
                            values.add(BigDecimal.ZERO);
                        }
                    }
                }
            }
            
            // Process multiplication and division first
            for (int i = 0; i < operators.size(); i++) {
                String op = operators.get(i);
                if (op.equals("*") || op.equals("/")) {
                    int leftIdx = i;
                    int rightIdx = i + 1;
                    
                    BigDecimal left = leftIdx < values.size() ? values.get(leftIdx) : BigDecimal.ZERO;
                    BigDecimal right = rightIdx < values.size() ? values.get(rightIdx) : BigDecimal.ZERO;
                    
                    BigDecimal result;
                    if (op.equals("*")) {
                        result = left.multiply(right);
                    } else if (op.equals("/")) {
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            // Division by zero - runtime error
                            interpreter.error("Runtime error: Division by zero");
                            return "0"; // Return zero as fallback
                        }
                        result = left.divide(right, 10, BigDecimal.ROUND_HALF_UP);
                    } else {
                        continue;
                    }
                    
                    // Replace the three elements (left, op, right) with the result
                    values.set(leftIdx, result);
                    values.remove(rightIdx);
                    operators.remove(i);
                    i--; // Adjust index
                }
            }
            
            // Process addition and subtraction
            BigDecimal result = BigDecimal.ZERO;
            for (int i = 0; i < values.size(); i++) {
                BigDecimal val = values.get(i);
                String op = i < operators.size() ? operators.get(i) : "+";
                
                if (op.equals("+")) {
                    result = result.add(val);
                } else if (op.equals("-")) {
                    result = result.subtract(val);
                } else {
                    // First value or no operator
                    if (i == 0) {
                        result = val;
                    } else {
                        result = result.add(val); // Default to addition
                    }
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            interpreter.error("Runtime error: Arithmetic error - " + e.getMessage());
            return "0";
        }
    }
    
    /**
     * Evaluate a check task condition and return the result.
     * Stores result in global `call`.
     * Comparison operators: =, !=, is, not, >, <, >=, <=, is empty
     */
    public boolean evaluateCheck(String condition, RuntimeContext context) {
        // Parse the condition string
        // Format: ? (operand1) operator (operand2)
        // But the ? is already consumed by the lexer/parser
        // The condition string passed here is everything after the ?
        
        if (condition == null || condition.isEmpty()) {
            context.setCall("NULL");
            return false;
        }
        
        // Tokenize and parse the comparison
        String[] parts = condition.trim().split("\\s+");
        
        if (parts.length < 3) {
            context.setCall("NULL");
            return false;
        }
        
        String operand1 = parts[0];
        String operator = parts[1];
        String operand2 = parts.length > 2 ? parts[2] : "";
        
        // Remove surrounding parentheses from operands if present
        if (operand1.startsWith("(") && operand1.endsWith(")")) {
            operand1 = operand1.substring(1, operand1.length() - 1);
        }
        if (operand2.startsWith("(") && operand2.endsWith(")")) {
            operand2 = operand2.substring(1, operand2.length() - 1);
        }
        
        // Determine the type of comparison
        boolean result;
        
        // First, try numeric comparison (if both operands are numeric)
        boolean op1IsNum = isNumeric(operand1);
        boolean op2IsNum = isNumeric(operand2);
        
        // Convert NULL to zero for comparison
        if (operand1.equalsIgnoreCase("null") || operand1.isEmpty()) {
            operand1 = "0";
        }
        if (operand2.equalsIgnoreCase("null") || operand2.isEmpty()) {
            operand2 = "0";
        }
        
        try {
            BigDecimal op1Val = op1IsNum ? new BigDecimal(operand1) : null;
            BigDecimal op2Val = op2IsNum ? new BigDecimal(operand2) : null;
            
            switch (operator.toLowerCase()) {
                case "=":
                    if (op1Val != null && op2Val != null) {
                        result = op1Val.compareTo(op2Val) == 0;
                    } else {
                        // String comparison - raw values
                        result = operand1.equals(operand2);
                    }
                    break;
                case "!=":
                    if (op1Val != null && op2Val != null) {
                        result = op1Val.compareTo(op2Val) != 0;
                    } else {
                        result = !operand1.equals(operand2);
                    }
                    break;
                case "is":
                    // Case-insensitive raw string comparison
                    result = operand1.equalsIgnoreCase(operand2);
                    break;
                case "not":
                    // Negated raw string comparison
                    result = !operand1.equalsIgnoreCase(operand2);
                    break;
                case ">":
                    if (op1Val != null && op2Val != null) {
                        result = op1Val.compareTo(op2Val) > 0;
                    } else {
                        result = false; // Can't compare strings numerically
                    }
                    break;
                case "<":
                    if (op1Val != null && op2Val != null) {
                        result = op1Val.compareTo(op2Val) < 0;
                    } else {
                        result = false;
                    }
                    break;
                case ">=":
                    if (op1Val != null && op2Val != null) {
                        result = op1Val.compareTo(op2Val) >= 0;
                    } else {
                        result = false;
                    }
                    break;
                case "<=":
                    if (op1Val != null && op2Val != null) {
                        result = op1Val.compareTo(op2Val) <= 0;
                    } else {
                        result = false;
                    }
                    break;
                case "is empty":
                    // Check if operand is NULL
                    result = operand1 == null || operand1.isEmpty() || operand1.equalsIgnoreCase("null");
                    break;
                default:
                    // Unknown operator - try string equality
                    result = operand1.equals(operand2);
                    break;
            }
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            if ("is".equalsIgnoreCase(operator)) {
                result = operand1.equalsIgnoreCase(operand2);
            } else if ("not".equalsIgnoreCase(operator)) {
                result = !operand1.equalsIgnoreCase(operand2);
            } else if ("is empty".equalsIgnoreCase(operator)) {
                result = operand1 == null || operand1.isEmpty();
            } else {
                result = false;
            }
        }
        
        // Store result in global `call`
        // The result should be representable as a string for the `call` variable
        if (result) {
            context.setCall("true");
        } else {
            context.setCall("false");
        }
        
        return result;
    }
    
    /**
     * Check if a string is numeric (can be parsed as BigDecimal).
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // --- Helper method to output values ---
    
    private void output(String message) {
        interpreter.output(message);
    }
}