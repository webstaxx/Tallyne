# Tallyne Language Specification & Implementation Report

**Draft 1.1 — November 2026**  
**Status:** Working language specification

---

## 1. Introduction

Tallyne is an experimental esoteric programming language built around the idea of programming as an endless checklist: a sequence of tasks, instructions, and routines.

Its defining characteristic is its checklist-inspired syntax and straightforward approach to programming. It is intended to be approachable and readable, rather than deliberately difficult like many traditional esoteric languages.

**Core philosophy:** Programming, one task at a time.

The language will be implemented in Java, using an interpreter rather than a compiler. Java is the implementation language; users write programs in Tallyne.

## 2. Fundamental Syntax

Tallyne uses a small set of structural symbols:

| Syntax | Purpose |
|---|---|
| `{NAME}` | Declares a Routine |
| `{NAME} with (A), (B)` | Declares a Routine with parameters |
| `#` | Begins an Action Task |
| `//` | Begins a comment |
| `# ***` | Ends a Routine or the program |
| `$` | Begins an Arithmetic Task |
| `?` | Begins a Check Task |

Tallyne is case-insensitive. Identifiers such as `HELLO` and `hello` refer to the same name.

### 2.1 Whitespace and Line Structure

This is a fundamental part of Tallyne's syntax.

- Spaces outside string literals are insignificant. They do not determine the meaning of a statement.
- Line breaks are significant and mandatory.
- Tallyne follows a line-by-line approach. Each task occupies its own logical line.
- Indentation has no syntactical significance.
- Indentation is optional and may be used for visual readability, but it is not recommended as a structural convention.
- Spaces inside string literals must be preserved exactly.

For example, both of these represent the same program:

```tallyne
{TASKS}
# store "Hello World" in (message)
# radio (message) to IO
# ***
```

```tallyne
{TASKS}
        # store "Hello World" in (message)
    # radio (message) to IO
# ***
```

The indentation does not change execution. The line breaks do.

**Implementation requirement:** The lexer and parser must preserve logical line boundaries. Whitespace handling must not accidentally remove spaces inside string literals or merge separate task lines.

## 3. Program Execution and Routines

A Routine is a named block of tasks.

The default entry point is:

```tallyne
{TASKS}
```

Execution begins from `{TASKS}` and proceeds sequentially.

### 3.1 Declaration Rules

- A Routine must be declared before it is called.
- A Routine cannot be declared inside another Routine.
- Duplicate Routine names produce a declaration error.
- Calling an undefined Routine produces an error.
- Routine names are case-insensitive.
- Routines may call other Routines.
- Recursion is supported.

Declarations are interpreted from top to bottom. The interpreter must not assume that a Routine declared later is already available.

### 3.2 Routine Termination

The syntax:

```tallyne
# ***
```

terminates the current Routine.

- In `{TASKS}`, it terminates the program.
- In another Routine, it returns control to the calling Routine.

## 4. Routine Parameters and Invocation

Routines may accept parameters:

```tallyne
{GREET} with (name)
# radio "Hello" to IO
# ***
```

Parameters are declared using parentheses.

Tallyne distinguishes three operations:

### 4.1 Radio

`radio` sends information to a Routine or Entity.

When directed to a Routine, it assigns parameter values without executing the Routine.

Values are positional and follow the order in which parameters were declared.

Repeated `radio` operations overwrite previously assigned values, beginning with the first parameter.

### 4.2 Tally

`tally` executes a Routine using its currently assigned parameter values.

It does not clear those values after execution. They remain available until overwritten.

### 4.3 Prompt

`prompt` assigns supplied values and immediately executes the Routine.

Unlike `tally`, it clears or reinitialises the Routine's parameter values before assigning the new ones.

An empty prompt produces an `Empty Prompt` error.

### 4.4 Missing and Extra Parameters

- Missing arguments are permitted.
- Missing parameters receive a NULL value, with context-dependent fallback behaviour.
- Extra arguments are ignored.

The exact NULL fallback model remains to be formally specified.

## 5. Variables and Data Flow

Tallyne has local variables and three global Key Variables:

- `it`
- `yield`
- `call`

### 5.1 Local Variables

Ordinary variables belong to their respective Routine.

When one Routine calls another, the caller's local variables remain intact. The called Routine operates with its own local variables.

Recursive calls must maintain separate local contexts.

### 5.2 Global Key Variables

The three Key Variables are shared globally. Changes made by one Routine are visible to other Routines.

Their established roles are:

| Key Variable | Purpose |
|---|---|
| `it` | Receives values from operations such as `return`, `pull`, `cut`, and Entity queries |
| `yield` | Receives arithmetic results |
| `call` | Receives Check Task results |

These variables are not interchangeable. The interpreter must preserve their distinct roles.

Whether users may explicitly overwrite these Key Variables remains to be confirmed.

### 5.3 NULL

Every variable exists in a NULL state.

NULL is context-dependent rather than a conventional fixed data type. It may behave differently depending on the operation in which it is used.

One established rule is:

- NULL is treated as zero in arithmetic.

The complete NULL behaviour and its interaction with different kinds of values remain open design decisions. The implementation must not impose an arbitrary conventional type system.

### 5.4 Return

The syntax:

```tallyne
# return (sum)
```

places the value of `(sum)` into the global variable `it`.

Importantly, `return` does **not** terminate execution. Subsequent tasks in the Routine continue executing.

## 6. Arithmetic Tasks

Arithmetic Tasks begin with `$`.

Example:

```tallyne
$ 10 + 5 * 2
```

Arithmetic follows PEMDAS precedence and stores its result in `yield`.

Established behaviour:

- Arithmetic results are stored in `yield`.
- NULL operands are treated as zero.
- Numeric strings are converted for arithmetic.
- The resulting value is stored in `yield` as a string.
- Numeric coercion is expected within Arithmetic Tasks, not as a universal conversion rule.

There are no separate `add`, `subtract`, `multiply`, or `divide` tasks. Arithmetic is handled through the `$` syntax.

The precise arithmetic grammar, modulus notation, division-by-zero behaviour, and numeric precision remain to be confirmed.

## 7. Lists

Tallyne supports list operations.

### 7.1 Declaration

```tallyne
# store ["red","green","blue"] in (colors)
```

An empty list is represented by:

```tallyne
[]
```

List indexing begins at zero.

### 7.2 List Operations

| Operation | Purpose |
|---|---|
| `pull` | Retrieves an element and places it in `it` |
| `insert ... at ...` | Inserts an element at a specified index |
| `insert ... into` | Appends an element |
| `mark` | Replaces an element at a specified index |
| `cut` | Removes an element and places the removed value in `it` |

Examples:

```tallyne
# pull 0 from (colors)
# insert "yellow" at 1 in (colors)
# insert "purple" into (colors)
# mark "orange" at 0 in (colors)
# cut 2 from (colors)
```

Nested lists, permitted element types, and invalid-index behaviour remain to be specified.

## 8. Entities

Entities are essential components of Tallyne. They are responsible for providing data and handling operations that Tallyne itself does not directly perform.

Entities are not simply conventional object-oriented objects. A useful analogy is that a Routine resembles a class in its organisational role, while an Entity can delegate underlying functionality to the host environment.

### 8.1 Entity Communication

Tallyne uses two important operations:

- `radio` sends information to an Entity.
- `query` requests information from an Entity and stores the response in `it`.

Example:

```tallyne
# query IRSensor with "distance"
# store it in (distance)
```

`query` is intended for Entities, not Routines.

### 8.2 Entity Categories

**Alive Entities**

- Active throughout program execution.
- Typically system-provided.
- `IO` is an example.

**Idle Entities**

- Activated when called.
- Typically user-defined.

The exact lifecycle rules are subject to further specification.

### 8.3 Entity Contract

The V1 interpreter must include a basic Entity integration boundary.

Its purpose is to allow the interpreter to accept, register, and communicate with an Entity through a defined interface.

This is deliberately a placeholder contract, not a complete third-party extension framework.

The implementation should leave room for future external Entities without building an entire package ecosystem prematurely.

## 9. Input and Output

`IO` is an Alive Entity responsible for basic input and output.

Examples:

```tallyne
# radio "Hello World" to IO
```

This sends output to the IO Entity.

```tallyne
# query IO
```

This requests input from the IO Entity.

The exact input/output formatting and error behaviour should be defined as part of the Entity contract.

## 10. Check Tasks and Conditionals

A Check Task begins with `?`.

Its result is stored in the global Key Variable `call`.

The known comparison operators are:

| Operator | Meaning |
|---|---|
| `=` | Numeric equality |
| `!=` | Numeric inequality |
| `is` | Case-insensitive raw string comparison |
| `not` | Negated raw string comparison |
| `>` | Greater than |
| `<` | Less than |
| `>=` | Greater than or equal to |
| `<=` | Less than or equal to |
| `is empty` | Checks whether a value is NULL |

Example:

```tallyne
? (age) >= (18)
```

The result is placed in `call`.

The language also supports conditional tasks using `if` and `else`.

An example from the existing syntax:

```tallyne
# if call is true, radio "Allowed" to IO
# else, radio "Denied" to IO
```

The exact grammar, association between checks and conditional tasks, and behaviour across multiple conditional lines still need to be formalised.

Tallyne does not have a conventional general-purpose Boolean data type. `call` is a special Key Variable used for Check Task results.

## 11. Loops

The currently established loop forms are:

```tallyne
# forever <TASK>
```

and:

```tallyne
# while (condition) do <TASK>
```

The first represents an infinite loop. The second repeats while its condition is true.

The exact grammar for multi-task loop bodies, nested loops, condition evaluation timing, and loop-control operations remain unresolved.

The interpreter must not invent additional loop syntax without approval.

## 12. Interpreter Architecture

Tallyne will be implemented in Java as an interpreter.

The proposed architecture is:

1. **Lexer:** Recognises Tallyne symbols, identifiers, literals, operators, and line boundaries.
2. **Parser:** Interprets the token stream and constructs the program representation.
3. **AST:** Represents Routines, tasks, expressions, and other language structures.
4. **Runtime:** Maintains execution state, variables, parameters, and call contexts.
5. **Interpreter:** Executes tasks according to Tallyne's semantics.
6. **Entity boundary:** Allows the interpreter to communicate with registered Entities.

The architecture must respect Tallyne's line-sensitive syntax. A parser that treats the entire source as an undifferentiated stream of whitespace-separated tokens would be unsuitable.

The implementation must also preserve sequential declaration visibility and separate local contexts during Routine calls and recursion.

## 13. Distribution and Execution

Tallyne should be usable through two interfaces.

### 13.1 Local CLI

Users should be able to install Tallyne and execute `.tly` files through a command-line interface.

Illustrative commands:

```bash
tallyne run hello.tly
tallyne list
tallyne unpack package.zip
```

These are CLI commands, not Tallyne language statements.

The initial Java implementation may be distributed as a JAR, requiring Java to be installed. Standalone executables for different operating systems may be considered later.

### 13.2 Browser Playground

A browser-based playground should allow users to:

- Write Tallyne code in an editor.
- Run their programs.
- View program output.
- Experiment without installing Tallyne locally.

### 13.3 Shared Language Semantics

The CLI and browser playground must use the same language semantics and interpreter behaviour.

They are different interfaces to Tallyne, not separate implementations of the language.

## 14. Compatibility and Future Versions

Tallyne should be designed with future extensibility in mind.

A planned V2 feature is support for external Entity packages.

The intended concept:

1. Third-party developers create Entities in Java.
2. Entities are distributed as ZIP packages.
3. Users install packages through the CLI, for example:

```bash
tallyne unpack WebBrowser.zip
```

4. A Tallyne program loads an installed Entity:

```tallyne
# load WebBrowser
```

This is a future compatibility goal, not a requirement to build a complete package manager in V1.

V1 should provide only the basic Entity contract and integration boundary necessary to make future expansion feasible.

## 15. Unresolved Language Decisions

The following points must be settled before the language specification can be considered complete.

| Area | Unresolved question |
|---|---|
| NULL | What is the intended context-dependent value model? |
| Key Variables | Can users explicitly overwrite `it`, `yield`, and `call`? |
| Arithmetic | What is the exact modulus notation? |
| Arithmetic | How are division by zero and numeric precision handled? |
| Lists | What values and nested structures are permitted? |
| Lists | What happens when an index is invalid? |
| Conditionals | What is the exact `if`/`else` grammar and association? |
| Loops | How are multi-task loop bodies represented? |
| Loops | Are nested loops supported, and how are they controlled? |
| Parameters | What are the exact NULL fallback rules for missing arguments? |
| Entities | What is the minimum formal Java Entity contract? |
| Line structure | Are empty lines ignored, and can any statement span multiple lines? |

These are not implementation details to be guessed by the developer. They are language-design decisions.

## 16. Implementation Principles

The implementation must follow these principles:

1. **Preserve the language identity.** Tallyne is a checklist-inspired esoteric language, not a general-purpose automation or robotics framework.
2. **Follow the specification.** Do not invent syntax or semantics to fill gaps.
3. **Respect line boundaries.** Newlines are significant; indentation is not.
4. **Preserve literal contents.** Spaces inside strings must remain intact.
5. **Maintain sequential visibility.** Routines become available only after their declarations are encountered.
6. **Preserve execution contexts.** Routine calls and recursion must not corrupt local variables.
7. **Keep Key Variables distinct.** `it`, `yield`, and `call` have separate established purposes.
8. **Keep V1 focused.** Implement the basic Entity contract, not the entire future package ecosystem.
9. **Maintain consistency.** CLI and browser playground execution must behave identically.
10. **Do not silently resolve ambiguities.** Ask for clarification whenever a language rule is missing or contradictory.

---

**End of Draft 1.1**

This document is the working reference for Tallyne's language design and implementation. Any unresolved decisions must be clarified before the corresponding behaviour is finalised in code.