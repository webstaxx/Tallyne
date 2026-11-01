package com.tallyne.model;

/**
 * Interface for all task nodes in the AST.
 */
public interface TaskNode {
    /**
     * Execute this task within the given runtime context.
     * Returns true if execution should continue, false if the routine should terminate.
     */
    boolean execute(RuntimeContext context);
    
    /**
     * Get the type of this task for debugging/display.
     */
    String getTaskType();
}

/**
 * Represents a Routine declaration: {NAME} or {NAME} with (PARAM1, PARAM2).
 */
public class RoutineNode {
    private String name;
    private boolean hasParameters;
    private List<String> parameterNames;
    private List<TaskNode> tasks;
    
    public RoutineNode(String name, boolean hasParameters, List<String> parameterNames) {
        this.name = name;
        this.hasParameters = hasParameters;
        this.parameterNames = parameterNames != null ? parameterNames : new ArrayList<>();
        this.tasks = new ArrayList<>();
    }
    
    public String getName() { return name; }
    public boolean hasParameters() { return hasParameters; }
    public List<String> getParameterNames() { return parameterNames; }
    public List<TaskNode> getTasks() { return tasks; }
    public void addTask(TaskNode task) { tasks.add(task); }
    public void setTasks(List<TaskNode> tasks) { this.tasks = tasks; }
}

/**
 * Abstract base class for arithmetic task nodes.
 */
public abstract class ArithmeticTaskNode implements TaskNode {
    protected String expression;
    
    public ArithmeticTaskNode(String expression) {
        this.expression = expression;
    }
    
    public String getExpression() { return expression; }
    public abstract String getTaskType();
}

/**
 * Represents an arithmetic task: $ expression
 * Stores result in `yield` as a string.
 */
public class ArithmeticTaskNode extends ArithmeticTaskNode {
    public ArithmeticTaskNode(String expression) {
        super(expression);
    }
    
    @Override
    public boolean execute(RuntimeContext context) {
        // Evaluate the arithmetic expression and store result in yield
        // This will be implemented in the Runtime class
        context.getInterpreter().getRuntime().evaluateArithmetic(expression, context);
        return true;
    }
    
    @Override
    public String getTaskType() { return "arithmetic"; }
}

/**
 * Represents a check task: ? condition
 * Stores result in global `call` variable.
 */
public class CheckTaskNode implements TaskNode {
    private String condition;
    
    public CheckTaskNode(String condition) {
        this.condition = condition;
    }
    
    public String getCondition() { return condition; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        // Evaluate the check condition and store result in `call`
        context.getInterpreter().getRuntime().evaluateCheck(condition, context);
        return true;
    }
    
    @Override
    public String getTaskType() { return "check"; }
}

/**
 * Represents a radio task: # radio (value) to IO or # radio (value) to {ROUTINE}
 * or # radio (value), (value) to {ROUTINE} (parameter assignment)
 */
public class RadioTaskNode implements TaskNode {
    private String target; // "IO" or routine name
    private String value;
    private boolean hasParameterList;
    private List<String> parameterValues;
    
    public RadioTaskNode(String target, String value, boolean hasParameterList, List<String> parameterValues) {
        this.target = target != null ? target : "";
        this.value = value != null ? value : "";
        this.hasParameterList = hasParameterList;
        this.parameterValues = parameterValues != null ? parameterValues : new ArrayList<>();
    }
    
    public String getTarget() { return target; }
    public String getValue() { return value; }
    public boolean hasParameterList() { return hasParameterList; }
    public List<String> getParameterValues() { return parameterValues; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeRadio(target, value, hasParameterList, parameterValues, context);
    }
    
    @Override
    public String getTaskType() { return "radio"; }
}

/**
 * Represents a store task: # store (value) in (variable)
 */
public class StoreTaskNode implements TaskNode {
    private String value;
    private String variable;
    
    public StoreTaskNode(String value, String variable) {
        this.value = value != null ? value : "";
        this.variable = variable != null ? variable : "";
    }
    
    public String getValue() { return value; }
    public String getVariable() { return variable; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeStore(value, variable, context);
    }
    
    @Override
    public String getTaskType() { return "store"; }
}

/**
 * Represents a return task: # return (sum)
 * Places value in `it` but does NOT terminate execution.
 */
public class ReturnTaskNode implements TaskNode {
    private String expression;
    
    public ReturnTaskNode(String expression) {
        this.expression = expression != null ? expression : "";
    }
    
    public String getExpression() { return expression; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeReturn(expression, context);
    }
    
    @Override
    public String getTaskType() { return "return"; }
}

/**
 * Represents a pull task: # pull (index) from (list)
 * Stores retrieved element in `it`.
 */
public class PullTaskNode implements TaskNode {
    private String index;
    private String listName;
    
    public PullTaskNode(String index, String listName) {
        this.index = index != null ? index : "";
        this.listName = listName != null ? listName : "";
    }
    
    public String getIndex() { return index; }
    public String getListName() { return listName; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executePull(index, listName, context);
    }
    
    @Override
    public String getTaskType() { return "pull"; }
}

/**
 * Represents a cut task: # cut (index) from (list)
 * Removes element and stores in `it`.
 */
public class CutTaskNode implements TaskNode {
    private String index;
    private String listName;
    
    public CutTaskNode(String index, String listName) {
        this.index = index != null ? index : "";
        this.listName = listName != null ? listName : "";
    }
    
    public String getIndex() { return index; }
    public String getListName() { return listName; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeCut(index, listName, context);
    }
    
    @Override
    public String getTaskType() { return "cut"; }
}

/**
 * Represents an insert task: # insert (value) at (index) in (list) or # insert (value) into (list)
 */
public class InsertTaskNode implements TaskNode {
    private String value;
    private String atIndex; // null if "into" (append), otherwise index string
    private String listName;
    
    public InsertTaskNode(String value, String atIndex, String listName) {
        this.value = value != null ? value : "";
        this.atIndex = atIndex;
        this.listName = listName != null ? listName : "";
    }
    
    public String getValue() { return value; }
    public String getAtIndex() { return atIndex; }
    public String getListName() { return listName; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeInsert(value, atIndex, listName, context);
    }
    
    @Override
    public String getTaskType() { return "insert"; }
}

/**
 * Represents a mark task: # mark (value) at (index) in (list)
 */
public class MarkTaskNode implements TaskNode {
    private String value;
    private String index;
    private String listName;
    
    public MarkTaskNode(String value, String index, String listName) {
        this.value = value != null ? value : "";
        this.index = index != null ? index : "";
        this.listName = listName != null ? listName : "";
    }
    
    public String getValue() { return value; }
    public String getIndex() { return index; }
    public String getListName() { return listName; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeMark(value, index, listName, context);
    }
    
    @Override
    public String getTaskType() { return "mark"; }
}

/**
 * Represents a query task: # query (entity) with (question)
 * Response stored in global `it`.
 */
public class QueryTaskNode implements TaskNode {
    private String entityName;
    private String question;
    
    public QueryTaskNode(String entityName, String question) {
        this.entityName = entityName != null ? entityName : "";
        this.question = question != null ? question : "";
    }
    
    public String getEntityName() { return entityName; }
    public String getQuestion() { return question; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeQuery(entityName, question, context);
    }
    
    @Override
    public String getTaskType() { return "query"; }
}

/**
 * Represents an if task: # if call is true, (task)
 */
public class IfTaskNode implements TaskNode {
    private String body;
    
    public IfTaskNode(String body) {
        this.body = body != null ? body : "";
    }
    
    public String getBody() { return body; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        // Check the global `call` variable and conditionally execute body
        return context.getInterpreter().getRuntime().executeIf(body, context);
    }
    
    @Override
    public String getTaskType() { return "if"; }
}

/**
 * Represents an else task: # else, (task)
 */
public class ElseTaskNode implements TaskNode {
    private String body;
    
    public ElseTaskNode(String body) {
        this.body = body != null ? body : "";
    }
    
    public String getBody() { return body; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeElse(body, context);
    }
    
    @Override
    public String getTaskType() { return "else"; }
}

/**
 * Represents a forever loop: # forever <task>
 */
public class ForeverTaskNode implements TaskNode {
    private String taskDescription;
    
    public ForeverTaskNode(String taskDescription) {
        this.taskDescription = taskDescription != null ? taskDescription : "";
    }
    
    public String getTaskDescription() { return taskDescription; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        // Execute the task indefinitely (with stack depth limit in interpreter)
        return context.getInterpreter().getRuntime().executeForever(taskDescription, context);
    }
    
    @Override
    public String getTaskType() { return "forever"; }
}

/**
 * Represents a while loop: # while (condition) do <task>
 */
public class WhileTaskNode implements TaskNode {
    private String condition;
    private String body;
    
    public WhileTaskNode(String condition, String body) {
        this.condition = condition != null ? condition : "";
        this.body = body != null ? body : "";
    }
    
    public String getCondition() { return condition; }
    public String getBody() { return body; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeWhile(condition, body, context);
    }
    
    @Override
    public String getTaskType() { return "while"; }
}

/**
 * Represents a routine call task: # tally {ROUTINE}, # prompt {ROUTINE} with params, # radio to {ROUTINE}
 */
public class RoutineCallTaskNode implements TaskNode {
    private String action; // "tally", "prompt", "radio"
    private String targetRoutine; // routine name
    private List<String> parameterValues;
    
    public RoutineCallTaskNode(String action, String targetRoutine, List<String> parameterValues) {
        this.action = action != null ? action : "tally";
        this.targetRoutine = targetRoutine != null ? targetRoutine : "";
        this.parameterValues = parameterValues != null ? parameterValues : new ArrayList<>();
    }
    
    public String getAction() { return action; }
    public String getTargetRoutine() { return targetRoutine; }
    public List<String> getParameterValues() { return parameterValues; }
    
    @Override
    public boolean execute(RuntimeContext context) {
        return context.getInterpreter().getRuntime().executeRoutineCall(action, targetRoutine, parameterValues, context);
    }
    
    @Override
    public String getTaskType() { return "routine_call"; }
}