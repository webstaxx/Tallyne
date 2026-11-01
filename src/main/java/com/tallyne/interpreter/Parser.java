package com.tallyne.interpreter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tallyne Line-Sensitive Parser.
 * 
 * Responsibilities:
 * - Parse token stream into an AST respecting logical line boundaries
 * - Track sequential declaration visibility (routines available after declaration)
 * - Handle parameter declaration and assignment (radio, tally, prompt)
 * - Construct RoutineNode, Task nodes, Expression nodes
 * - Case-insensitive routine name matching
 * - Track line numbers for error reporting
 * - Do NOT merge tokens across line boundaries in ways that alter meaning
 */
public class Parser {

    private List<Token> tokens;
    private int position;
    private int lineNumber;
    private RoutineNode currentRoutine;
    private List<RoutineNode> programRoutines;
    private boolean inRoutine;
    private String currentRoutineName;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.lineNumber = 1;
        this.programRoutines = new ArrayList<>();
        this.inRoutine = false;
        this.currentRoutineName = "";
    }

    /**
     * Parse the entire token stream and produce a list of RoutineNodes.
     * The first routine encountered becomes the default entry point if it's {TASKS}.
     */
    public List<RoutineNode> parse() {
        programRoutines.clear();
        position = 0;
        lineNumber = 1;

        while (position < tokens.size()) {
            Token token = tokens.get(position);
            
            if (token.getType().equals(Lexer.TOK_EOL)) {
                position++;
                lineNumber++;
                continue;
            }
            
            if (token.getType().equals(Lexer.TOK_EOF)) {
                break;
            }
            
            if (token.getType().equals(Lexer.TOK_LeftBrace)) {
                // Routine declaration: {NAME} or {NAME} with (A), (B)
                position++;
                currentRoutine = parseRoutineDeclaration();
                if (currentRoutine != null) {
                    programRoutines.add(currentRoutine);
                    // If this is the first routine and it's {TASKS}, it's the entry point
                }
                inRoutine = currentRoutine != null;
                if (inRoutine) {
                    currentRoutineName = currentRoutine.getName().toLowerCase();
                }
                continue;
            }
            
            // Skip any unexpected tokens at top level
            position++;
        }
        
        return programRoutines;
    }

    /**
     * Parse a routine declaration: {NAME} or {NAME} with (PARAM1, PARAM2)
     */
    private RoutineNode parseRoutineDeclaration() {
        // Expect left brace already consumed
        String routineName = null;
        boolean hasParameters = false;
        List<String> parameterNames = new ArrayList<>();
        
        // Read the routine name (identifier)
        if (position < tokens.size()) {
            Token nameToken = tokens.get(position);
            if (nameToken.getType().equals(Lexer.TOK_Ident)) {
                routineName = nameToken.getValue();
                position++;
            } else {
                // Error: expected routine name
                return null;
            }
        }
        
        // Check for "with" parameter declaration
        if (position < tokens.size() && tokens.get(position).getType().equals(Lexer.TOK_Ident) 
                && tokens.get(position).getValue().equalsIgnoreCase("with")) {
            position++; // consume "with"
            
            // Expect left parenthesis
            if (position < tokens.size() && tokens.get(position).getType().equals(Lexer.TOK_LeftParen)) {
                position++; // consume "( "
                
                // Parse parameter list until right paren
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    Token paramToken = tokens.get(position);
                    if (paramToken.getType().equals(Lexer.TOK_Ident)) {
                        parameterNames.add(paramToken.getValue());
                        position++;
                    }
                    // Skip commas between parameters
                    if (position < tokens.size() && tokens.get(position).getType().equals(Lexer.TOK_Comma)) {
                        position++;
                    }
                }
                
                // Consume right paren
                if (position < tokens.size() && tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    position++;
                }
                
                hasParameters = true;
            }
        }
        
        // The routine body consists of tasks until # ***
        // We need to collect tasks until we hit # *** or EOL at top level
        // For now, create the routine node; tasks will be parsed on demand
        
        RoutineNode routine = new RoutineNode(routineName, hasParameters, parameterNames);
        currentRoutine = routine;
        return routine;
    }

    /**
     * Parse tasks within a routine body until # *** or end of source.
     * This is called after a routine declaration to parse its task list.
     */
    public List<TaskNode> parseRoutineBody() {
        List<TaskNode> tasks = new ArrayList<>();
        
        while (position < tokens.size()) {
            Token token = tokens.get(position);
            
            if (token.getType().equals(Lexer.TOK_EOL)) {
                position++;
                lineNumber++;
                continue;
            }
            
            if (token.getType().equals(Lexer.TOK_HashStar)) {
                // # *** terminates the current routine
                position++;
                break;
            }
            
            if (token.getType().equals(Lexer.TOK_EOF)) {
                break;
            }
            
            // Parse different task types based on prefix
            TaskNode task = parseTask();
            if (task != null) {
                tasks.add(task);
            } else {
                // Skip unknown token, advance
                position++;
            }
        }
        
        return tasks;
    }

    /**
     * Parse a single task based on its prefix token.
     */
    private TaskNode parseTask() {
        if (position >= tokens.size()) {
            return null;
        }
        
        Token token = tokens.get(position);
        
        // Arithmetic task: $ expression
        if (token.getType().equals(Lexer.TOK_ArithmeticTask)) {
            position++;
            return parseArithmeticTask();
        }
        
        // Check task: ? condition
        if (token.getType().equals(Lexer.TOK_CheckTask)) {
            position++;
            return parseCheckTask();
        }
        
        // Action tasks starting with #
        if (token.getType().equals(Lexer.TOK_ActionTask)) {
            position++;
            return parseActionTask();
        }
        
        // String literal or number as a task? Possibly a store task
        if (token.getType().equals(Lexer.TOK_StringLiteral) || token.getType().equals(Lexer.TOK_Number)) {
            // Could be # store "value" in (var) or similar
            // Look at the next tokens to determine
            return parseStoreTask();
        }
        
        // Identifier - could be a routine call via tally/radio/prompt
        if (token.getType().equals(Lexer.TOK_Ident)) {
            // Look ahead to see if this is a routine call pattern
            // e.g., # radio "Hello" to {ROUTINE} or {ROUTINE} with params
            // Or just # radio "Hello", "World" to {ROUTINE}
            return parseRoutineCallTask();
        }
        
        return null;
    }

    /**
     * Parse an arithmetic task: $ expression
     * Follows PEMDAS precedence
     */
    private TaskNode parseArithmeticTask() {
        // Collect tokens until we hit EOL or another task prefix
        // For now, parse a simple expression
        StringBuilder expression = new StringBuilder();
        
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (!t.getType().equals(Lexer.TOK_Number) && !t.getType().equals(Lexer.TOK_Ident) 
                && !t.getType().equals(Lexer.TOK_LeftParen) && !t.getType().equals(Lexer.TOK_RightParen)
                && !t.getType().equals(Lexer.TOK_Comma)) {
                break;
            }
            
            expression.append(t.getValue()).append(" ");
            position++;
        }
        
        // Trim trailing space
        if (expression.length() > 0) {
            expression.setLength(expression.length() - 1);
        }
        
        return new ArithmeticTaskNode(expression.toString().trim());
    }

    /**
     * Parse a check task: ? (operand1) operator (operand2)
     */
    private TaskNode parseCheckTask() {
        StringBuilder condition = new StringBuilder();
        
        // Parse until EOL or # ***
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            condition.append(t.getValue()).append(" ");
            position++;
        }
        
        if (condition.length() > 0) {
            condition.setLength(condition.length() - 1);
        }
        
        return new CheckTaskNode(condition.toString().trim());
    }

    /**
     * Parse an action task (# prefix).
     * Handles: radio, store, return, pull, cut, insert, mark, query, if/else, forever, while
     */
    private TaskNode parseActionTask() {
        if (position >= tokens.size()) {
            return null;
        }
        
        Token firstToken = tokens.get(position);
        
        // Look at the word after # to determine task type
        // We need to look at the next non-EOL token
        int lookaheadPos = position;
        String taskType = null;
        
        // Skip EOL tokens in looking ahead
        while (lookaheadPos < tokens.size() && tokens.get(lookaheadPos).getType().equals(Lexer.TOK_EOL)) {
            lookaheadPos++;
        }
        
        if (lookaheadPos < tokens.size()) {
            Token lookahead = tokens.get(lookaheadPos);
            taskType = lookahead.getValue().toLowerCase();
        }
        
        switch (taskType) {
            case "radio":
                position++; // consume the # token already consumed
                return parseRadioTask();
            case "store":
                position++;
                return parseStoreTask();
            case "return":
                position++;
                return parseReturnTask();
            case "pull":
                position++;
                return parsePullTask();
            case "cut":
                position++;
                return parseCutTask();
            case "insert":
                position++;
                return parseInsertTask();
            case "mark":
                position++;
                return parseMarkTask();
            case "query":
                position++;
                return parseQueryTask();
            case "if":
                position++;
                return parseIfTask();
            case "else":
                position++;
                return parseElseTask();
            case "forever":
                position++;
                return parseForeverTask();
            case "while":
                position++;
                return parseWhileTask();
            default:
                // Unknown action task, try to parse generically
                position++;
                return null;
        }
    }

    private TaskNode parseRadioTask() {
        // # radio (value) to IO or # radio (value) to {ROUTINE}
        // or # radio (value), (value) to {ROUTINE}
        
        StringBuilder value = new StringBuilder();
        String target = null; // "IO" or routine name
        boolean hasParameterList = false;
        List<String> paramValues = new ArrayList<>();
        
        // Parse until EOL or # ***
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (t.getType().equals(Lexer.TOK_StringLiteral)) {
                value.append(t.getValue()).append(" ");
                position++;
            } else if (t.getType().equals(Lexer.TOK_Ident)) {
                String ident = t.getValue().toLowerCase();
                
                // Check if this is a routine name target
                if (target == null && !ident.equals("to")) {
                    // Could be a routine name
                    target = ident;
                    position++;
                    // Check for "to" keyword
                    if (position < tokens.size() && tokens.get(position).getType().equals(Lexer.TOK_Ident) 
                            && tokens.get(position).getValue().equalsIgnoreCase("to")) {
                        position++; // consume "to"
                        // After "to", we might have IO or a routine name
                        if (position < tokens.size()) {
                            Token afterTo = tokens.get(position);
                            if (afterTo.getValue().equalsIgnoreCase("io")) {
                                target = "IO";
                                position++;
                            } else {
                                // It's a routine name
                                target = afterTo.getValue();
                                position++;
                            }
                        }
                    }
                    hasParameterList = true;
                    paramValues.add(ident);
                } else if (hasParameterList) {
                    paramValues.add(t.getValue());
                    position++;
                } else {
                    position++;
                }
            } else if (t.getType().equals(Lexer.TOK_LeftParen)) {
                position++;
                // Parse parenthesized value
                StringBuilder parenContent = new StringBuilder();
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    parenContent.append(tokens.get(position).getValue()).append(" ");
                    position++;
                }
                if (position < tokens.size()) {
                    position++; // consume right paren
                }
                value.append(parenContent.toString().trim());
            } else if (t.getType().equals(Lexer.TOK_Comma)) {
                position++;
                hasParameterList = true;
            } else {
                position++;
            }
        }
        
        if (value.length() > 0) {
            value.setLength(value.length() - 1);
        }
        
        return new RadioTaskNode(target, value.toString().trim(), hasParameterList, paramValues);
    }

    private TaskNode parseStoreTask() {
        // # store "value" in (variable) or # store it in (variable)
        
        String value = "";
        String variable = "";
        boolean hasValue = false;
        
        // Parse until EOL or # ***
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (t.getType().equals(Lexer.TOK_StringLiteral)) {
                value = t.getValue().substring(1, t.getValue().length() - 1); // strip quotes
                hasValue = true;
                position++;
            } else if (t.getType().equals(Lexer.TOK_Ident)) {
                // Could be a variable name (var) or the word "in"
                if (!variable.isEmpty() || !t.getValue().equalsIgnoreCase("in")) {
                    variable = t.getValue();
                }
                position++;
            } else if (t.getType().equals(Lexer.TOK_LeftParen)) {
                // Starting a parenthesized variable name like (message)
                // Skip until right paren
                StringBuilder varContent = new StringBuilder();
                position++; // consume (
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    varContent.append(tokens.get(position).getValue()).append(" ");
                    position++;
                }
                if (position < tokens.size()) {
                    position++; // consume )
                }
                variable = varContent.toString().trim();
            } else {
                position++;
            }
        }
        
        return new StoreTaskNode(hasValue ? value : "", variable);
    }

    private TaskNode parseReturnTask() {
        // # return (sum)
        String expression = "";
        
        // Parse until EOL or # ***
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (t.getType().equals(Lexer.TOK_LeftParen)) {
                // Parse parenthesized expression
                StringBuilder expr = new StringBuilder();
                position++; // consume (
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    expr.append(tokens.get(position).getValue()).append(" ");
                    position++;
                }
                if (position < tokens.size()) {
                    position++; // consume )
                }
                expression = expr.toString().trim();
                break;
            } else {
                // Could be a number or ident
                if (!t.getType().equals(Lexer.TOK_RightParen) && !t.getType().equals(Lexer.TOK_Comma)) {
                    expression += t.getValue() + " ";
                }
                position++;
            }
        }
        
        if (expression.length() > 0) {
            expression = expression.trim();
        }
        
        return new ReturnTaskNode(expression);
    }

    private TaskNode parsePullTask() {
        // # pull (index) from (list)
        String index = "";
        String listName = "";
        
        // Parse until EOL or # ***
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (t.getType().equals(Lexer.TOK_LeftParen)) {
                // Parse parenthesized content
                StringBuilder content = new StringBuilder();
                position++; // consume (
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    content.append(tokens.get(position).getValue()).append(" ");
                    position++;
                }
                if (position < tokens.size()) {
                    position++; // consume )
                }
                String parsed = content.toString().trim();
                
                // First parenthesized group is the index, second is the list name
                if (index.isEmpty()) {
                    index = parsed;
                } else {
                    listName = parsed;
                }
            } else {
                position++;
            }
        }
        
        return new PullTaskNode(index, listName);
    }

    private TaskNode parseCutTask() {
        // # cut (index) from (list) - same structure as pull
        String index = "";
        String listName = "";
        
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (t.getType().equals(Lexer.TOK_LeftParen)) {
                StringBuilder content = new StringBuilder();
                position++; // consume (
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    content.append(tokens.get(position).getValue()).append(" ");
                    position++;
                }
                if (position < tokens.size()) {
                    position++; // consume )
                }
                String parsed = content.toString().trim();
                
                if (index.isEmpty()) {
                    index = parsed;
                } else {
                    listName = parsed;
                }
            } else {
                position++;
            }
        }
        
        return new CutTaskNode(index, listName);
    }

    private TaskNode parseInsertTask() {
        // # insert (value) at (index) in (list) or # insert (value) into (list)
        String value = "";
        String atIndex = "";
        String listName = "";
        
        // Parse until EOL or # ***
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (t.getType().equals(Lexer.TOK_LeftParen)) {
                StringBuilder content = new StringBuilder();
                position++; // consume (
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    content.append(tokens.get(position).getValue()).append(" ");
                    position++;
                }
                if (position < tokens.size()) {
                    position++; // consume )
                }
                String parsed = content.toString().trim();
                
                // Determine which parenthesized group based on context
                // "at" signals index, otherwise it's value or list
                if (!atIndex.isEmpty()) {
                    if (atIndex.equalsIgnoreCase("at")) {
                        // Next paren after "at" is the index
                        // Actually we need to track position
                    }
                    atIndex = parsed;
                } else if (value.isEmpty()) {
                    value = parsed;
                } else {
                    listName = parsed;
                }
            } else if (t.getType().equals(Lexer.TOK_Ident) && t.getValue().equalsIgnoreCase("at")) {
                // Next parenthesized group is the index
                atIndex = "at";
                position++;
            } else if (t.getType().equals(Lexer.TOK_Ident) && t.getValue().equalsIgnoreCase("into")) {
                // "into" means append - the next paren is the list
                listName = ""; // will be set by next paren
                position++;
            } else {
                position++;
            }
        }
        
        return new InsertTaskNode(value, atIndex, listName);
    }

    private TaskNode parseMarkTask() {
        // # mark (value) at (index) in (list)
        String value = "";
        String index = "";
        String listName = "";
        
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (t.getType().equals(Lexer.TOK_LeftParen)) {
                StringBuilder content = new StringBuilder();
                position++; // consume (
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    content.append(tokens.get(position).getValue()).append(" ");
                    position++;
                }
                if (position < tokens.size()) {
                    position++; // consume )
                }
                String parsed = content.toString().trim();
                
                // First paren is value, second is index, third is list name
                if (!value.isEmpty()) {
                    if (index.isEmpty()) {
                        index = parsed;
                    } else {
                        listName = parsed;
                    }
                } else {
                    value = parsed;
                }
            } else {
                position++;
            }
        }
        
        return new MarkTaskNode(value, index, listName);
    }

    private TaskNode parseQueryTask() {
        // # query (entity) with (question)
        String entityName = "";
        String question = "";
        
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (t.getType().equals(Lexer.TOK_LeftParen)) {
                StringBuilder content = new StringBuilder();
                position++; // consume (
                while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                    content.append(tokens.get(position).getValue()).append(" ");
                    position++;
                }
                if (position < tokens.size()) {
                    position++; // consume )
                }
                String parsed = content.toString().trim();
                
                if (entityName.isEmpty()) {
                    entityName = parsed;
                } else {
                    question = parsed;
                }
            } else if (t.getType().equals(Lexer.TOK_Ident) && t.getValue().equalsIgnoreCase("with")) {
                // Next paren is the question
                position++;
                // Parse the question in parens
                if (position < tokens.size() && tokens.get(position).getType().equals(Lexer.TOK_LeftParen)) {
                    StringBuilder qContent = new StringBuilder();
                    position++; // consume (
                    while (position < tokens.size() && !tokens.get(position).getType().equals(Lexer.TOK_RightParen)) {
                        qContent.append(tokens.get(position).getValue()).append(" ");
                        position++;
                    }
                    if (position < tokens.size()) {
                        position++; // consume )
                    }
                    question = qContent.toString().trim();
                }
            } else {
                position++;
            }
        }
        
        return new QueryTaskNode(entityName, question);
    }

    private TaskNode parseIfTask() {
        // # if call is true, (task)
        // For V1, we parse the condition check association
        StringBuilder body = new StringBuilder();
        
        // Parse until EOL or # ***
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            body.append(t.getValue()).append(" ");
            position++;
        }
        
        if (body.length() > 0) {
            body.setLength(body.length() - 1);
        }
        
        return new IfTaskNode(body.toString().trim());
    }

    private TaskNode parseElseTask() {
        // # else, (task) or just # else
        StringBuilder body = new StringBuilder();
        
        // Parse until EOL or # ***
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            body.append(t.getValue()).append(" ");
            position++;
        }
        
        if (body.length() > 0) {
            body.setLength(body.length() - 1);
        }
        
        return new ElseTaskNode(body.toString().trim());
    }

    private TaskNode parseForeverTask() {
        // # forever <task>
        String taskDescription = "";
        
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            taskDescription += t.getValue() + " ";
            position++;
        }
        
        if (taskDescription.length() > 0) {
            taskDescription = taskDescription.trim();
        }
        
        return new ForeverTaskNode(taskDescription);
    }

    private TaskNode parseWhileTask() {
        // # while (condition) do <task>
        StringBuilder condition = new StringBuilder();
        StringBuilder body = new StringBuilder();
        
        // Parse the condition in parens
        boolean inCondition = true;
        while (position < tokens.size()) {
            Token t = tokens.get(position);
            
            if (t.getType().equals(Lexer.TOK_EOL) || t.getType().equals(Lexer.TOK_HashStar) 
                || t.getType().equals(Lexer.TOK_RightBrace)) {
                break;
            }
            
            if (inCondition && t.getType().equals(Lexer.TOK_LeftParen)) {
                inCondition = false;
                position++; // consume (
                continue;
            }
            
            if (!inCondition && t.getType().equals(Lexer.TOK_RightParen)) {
                inCondition = true;
                position++; // consume )
                // Expect "do" keyword
                if (position < tokens.size() && tokens.get(position).getType().equals(Lexer.TOK_Ident) 
                        && tokens.get(position).getValue().equalsIgnoreCase("do")) {
                    position++;
                }
                continue;
            }
            
            if (inCondition) {
                condition.append(t.getValue()).append(" ");
            } else {
                body.append(t.getValue()).append(" ");
            }
            position++;
        }
        
        if (condition.length() > 0) {
            condition.setLength(condition.length() - 1);
        }
        if (body.length() > 0) {
            body.setLength(body.length() - 1);
        }
        
        return new WhileTaskNode(condition.toString().trim(), body.toString().trim());
    }

    /**
     * Parse a store task from string/number token (when identifier isn't a routine call).
     */
    private TaskNode parseStoreTask() {
        // Generic store task parsing
        return new StoreTaskNode("", "");
    }

    /**
     * Parse a routine call task: # radio/prompt/tally to/with/{ROUTINE}
     */
    private TaskNode parseRoutineCallTask() {
        // This handles patterns like:
        # radio "Hello", "World" to {GREETING}
        # prompt {GREETING} with "Hello", "World"
        # tally {GREETING}
        
        // For now, consume and return a generic task
        // The detailed parameter handling will be in the runtime
        String target = "";
        String action = "call"; // radio, tally, prompt
        List<String> paramValues = new ArrayList<>();
        
        // Simple approach: look at the ident value
        // If the ident is a routine name in braces {NAME}, it's a routine reference
        // Otherwise, it might be a parameter assignment
        
        return new RoutineCallTaskNode(action, target, paramValues);
    }
}