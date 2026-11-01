package com.tallyne.interpreter;

import com.tallyne.model.*;

/**
 * RuntimeContext holds the execution state for a single routine execution.
 * Contains references to the interpreter, global variables, local variables,
 * parameter values, and the entity boundary for Entity communication.
 */
public class RuntimeContext {
    private TallyneInterpreter interpreter;
    private Runtime runtime;
    private RoutineNode currentRoutine;
    private ExecutionState state;
    
    // Global Key Variables (shared across all routines)
    private String it;        // General operation results
    private String yield;     // Arithmetic results
    private String call;      // Check task results
    
    // Local variables for the current routine
    private java.util.Map<String, String> localVariables;
    
    // Parameter values for the current routine
    private java.util.List<String> parameterValues;
    
    // Entity boundary for communication
    private EntityBoundary entityBoundary;
    
    // Call stack for recursion tracking
    private java.util.Deque<String> callStack;
    
    // Maximum recursion depth to prevent infinite recursion
    private static final int MAX_RECURSION_DEPTH = 64;
    
    public RuntimeContext(TallyneInterpreter interpreter, Runtime runtime) {
        this.interpreter = interpreter;
        this.runtime = runtime;
        this.state = new ExecutionState();
        this.it = null;
        this.yield = null;
        this.call = null;
        this.localVariables = new java.util.HashMap<>();
        this.parameterValues = new java.util.ArrayList<>();
        this.callStack = new java.util.ArrayDeque<>();
        this.entityBoundary = interpreter.getEntityBoundary();
    }
    
    // --- Global Key Variable accessors ---
    
    public String getIt() { return it; }
    public void setIt(String value) { this.it = value; }
    
    public String getYield() { return yield; }
    public void setYield(String value) { this.yield = value; }
    
    public String getCall() { return call; }
    public void setCall(String value) { this.call = value; }
    
    // --- Local variable accessors ---
    
    public String getLocalVariable(String name) {
        return localVariables.get(name);
    }
    
    public void setLocalVariable(String name, String value) {
        localVariables.put(name, value);
    }
    
    public void unsetLocalVariable(String name) {
        localVariables.remove(name);
    }
    
    // --- Parameter accessors ---
    
    public String getParameter(int index) {
        if (index >= 0 && index < parameterValues.size()) {
            return parameterValues.get(index);
        }
        return null; // Missing parameter = NULL
    }
    
    public int getParameterCount() {
        return parameterValues.size();
    }
    
    public void setParameter(int index, String value) {
        if (index >= 0) {
            while (parameterValues.size() <= index) {
                parameterValues.add(null);
            }
            parameterValues.set(index, value);
        }
    }
    
    public void clearParameters() {
        parameterValues.clear();
    }
    
    // --- Entity boundary ---
    
    public EntityBoundary getEntityBoundary() { return entityBoundary; }
    
    // --- Call stack ---
    
    public boolean pushCallStack(String routineName) {
        if (callStack.size() >= MAX_RECURSION_DEPTH) {
            interpreter.error("Runtime error: Maximum recursion depth exceeded");
            return false;
        }
        return callStack.push(routineName);
    }
    
    public String popCallStack() {
        return callStack.pop();
    }
    
    public int getCallStackDepth() {
        return callStack.size();
    }
    
    // --- Execution state ---
    
    public ExecutionState getState() { return state; }
    public void setState(ExecutionState state) { this.state = state; }
    
    // --- The current routine ---
    
    public RoutineNode getCurrentRoutine() { return currentRoutine; }
    public void setCurrentRoutine(RoutineNode routine) { this.currentRoutine = routine; }
    
    // --- Interpreter reference ---
    
    public TallyneInterpreter getInterpreter() { return interpreter; }
}