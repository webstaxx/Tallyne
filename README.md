# Tallyne

**Programming, one task at a time.**

Tallyne is an experimental esoteric programming language built around a simple idea: what if programming felt like completing a checklist?

Programs are organised into **Routines**, and each instruction is written as a **Task**. Tasks are executed sequentially, one after another, giving Tallyne a straightforward and readable structure.

Tallyne is designed to be approachable, expressive, and fun to experiment with. Its identity lies in its syntax and programming experience rather than solving a particular real-world problem.

---

## 1. Language Fundamentals

A few conventions form the foundation of Tallyne:

| Syntax | Purpose |
|---|---|
| `{NAME}` | Declares or references a Routine |
| `#` | Prefix for an Action Task |
| `$` | Prefix for an Arithmetic Task |
| `?` | Prefix for a Check Task |
| `//` | Begins a comment |
| `# ***` | Terminates a Routine |
| `(variable)` | References a variable |
| `"value"` | Represents a literal value |
| `[ ]` | Represents a List |

### Basic Rules

- Tallyne is case-insensitive. `HELLO` and `hello` are treated identically.
- Every program begins execution from the `{TASKS}` Routine.
- Tasks execute sequentially, from top to bottom.
- A Routine must be declared before it is called.
- Variables are local to their respective Routines, while certain system variables are shared globally.

---

## 2. Routines

Routines are the fundamental organisational units of Tallyne. They group Tasks together into reusable sequences of instructions.

### Declaring a Routine

A Routine is declared using curly braces:

```tallyne
{GREETING}
    # radio "Hello World" to IO
# ***
```

`# ***` marks the end of the current Routine.

The `{TASKS}` Routine is the program's default entry point. When it reaches `# ***`, program execution ends.

### Routine Parameters

Routines can accept parameters through the `with` keyword.

```tallyne
{ADDITION} with (A), (B)
    $ (A) + (B)
    # return yield
# ***
```

Parameters behave like local variables and can be accessed inside the Routine.

### Calling Routines

Tallyne provides three ways to interact with a Routine.

#### `radio` — Assign Values

`radio` sends information to a Routine without executing it.

```tallyne
# radio "Hello", "World" to {GREETING}
```

Values are assigned positionally, following the order in which parameters were declared.

Sending another `radio` instruction overwrites the previously assigned parameter values, starting from the first parameter.

#### `tally` — Execute

`tally` executes a Routine using its currently assigned parameter values.

```tallyne
# tally {GREETING}
```

The existing parameter values are retained after execution.

#### `prompt` — Assign and Execute

`prompt` supplies parameter values and immediately executes the Routine.

```tallyne
# prompt {GREETING} with "Hello", "World"
```

Unlike `radio`, `prompt` resets the Routine's parameter values before assigning the supplied values.

Missing arguments are permitted and receive NULL values. Extra arguments are ignored.

Using `prompt` without supplying parameters results in an `Empty Prompt` error.

### Routine Behaviour

- Routines can call other Routines.
- Recursion is supported.
- Routine declarations cannot be nested inside other Routines.
- Each Routine maintains its own local variables.
- Calling another Routine does not erase the caller's local variables.
- The global variables `it`, `yield`, and `call` are shared between Routines.

---

## 3. Entities

Entities are components that participate in Tallyne's data flow. They are somewhat analogous to objects in conventional programming languages, although they do not follow exactly the same model.

Examples include:

- `IO` — Input and output.
- `LeftMotor` — A motor Entity.
- `IRSensor` — An infrared sensor Entity.
- `Camera` — A camera Entity.

Entities can be classified according to their state:

**Alive Entities**

Remain active throughout program execution. System Entities such as `IO` belong to this category.

**Idle Entities**

Remain inactive until explicitly called upon. User-defined Entities generally belong to this category.

Entities and Routines serve different purposes: Routines organise program instructions, while Entities provide access to external components and operations.

---

## 4. Data Flow

Tallyne uses three special global variables, called **Key Variables**, to manage information between Tasks and Routines.

| Key Variable | Purpose |
|---|---|
| `it` | Holds general operation results and returned values |
| `yield` | Holds the result of an Arithmetic Task |
| `call` | Holds the result of a Check Task |

These variables can be referenced without parentheses.

### `it`

`it` is Tallyne's general-purpose data variable. It is used to hold values returned by operations and Routines.

For example:

```tallyne
{CALCULATE}
    $ 10 + 20
    # return yield
# ***

{TASKS}
    # tally {CALCULATE}
    # radio it to IO
# ***
```

### `yield`

`yield` stores the result of the most recently executed Arithmetic Task.

### `call`

`call` stores the result of the most recently executed Check Task. It is used by conditional Action Tasks to determine program flow.

---

## 5. Storage and Variables

Variables are declared and referenced using parentheses.

```tallyne
# store "Hello" in (message)
# store it in (result)
```

The first instruction stores a literal value. The second stores the current value of `it`.

Every variable exists in a NULL state until it receives a value. Tallyne does not use a conventional uninitialised-variable state.

NULL can behave differently depending on the context in which it is used.

For example, NULL is treated as zero during arithmetic operations.

Variables are local to their Routine. The three Key Variables are global.

---

## 6. Lists

A List is an ordered collection of values enclosed in square brackets.

Tallyne uses **zero-based indexing**, meaning the first element is at index `0`.

### Creating Lists

```tallyne
// Creating a populated List
# store ["red", "green", "blue"] in (colors)

// Creating an empty List
# store [] in (readings)
```

### List Management

| Task | Syntax | Behaviour |
|---|---|---|
| Pull | `# pull 1 from (colors)` | Retrieves an element into `it` |
| Insert at | `# insert "yellow" at 1 in (colors)` | Inserts an element at an index |
| Insert into | `# insert "yellow" into (colors)` | Appends an element |
| Mark | `# mark "yellow" at 1 in (colors)` | Replaces an existing element |
| Cut | `# cut 1 from (colors)` | Removes an element and stores it in `it` |

### Example

```tallyne
{LIST_DEMO}
    # store ["apple", "banana", "kiwi"] in (fruits)

    # pull 1 from (fruits)
    # radio it to IO

    # mark "MANGO" at 0 in (fruits)
    # insert "grape" at 1 in (fruits)
    # insert "orange" into (fruits)

    # cut 3 from (fruits)
# ***
```

---

## 7. Communication and Input/Output

Tallyne uses two principal instructions for communication:

### `radio`

Sends information to an Entity or Routine.

```tallyne
# radio "Hello World" to IO
```

When directed to `IO`, the output is produced immediately because `IO` is an Alive Entity.

### `query`

Requests information from an Entity.

```tallyne
# query IO with "What is your name?"
```

The response is stored in `it`.

For example, a program can request a sensor reading:

```tallyne
# query IRSensor with "distance"
# store it in (distance)
```

`query` is intended for requesting information from Entities, rather than invoking Routines.

---

## 8. Arithmetic

Tallyne uses the `$` prefix to identify Arithmetic Tasks.

An Arithmetic Task evaluates a mathematical expression according to the standard order of operations (**PEMDAS**) and stores the result in `yield`.

```tallyne
{CALCULATE}
    $ 4 * 4 + (12 + 3) * 10
    # radio yield to IO
# ***
```

Output:

```text
166
```

### Arithmetic Behaviour

- Standard mathematical precedence is followed.
- NULL values are treated as zero.
- Numeric strings are converted for arithmetic calculations.
- The calculated result is stored in `yield` as a string.

Arithmetic is handled through expressions rather than separate addition, subtraction, multiplication, division, or modulus Tasks.

---

## 9. Loops

Tallyne provides two basic loop structures.

### Forever

Repeats a Task indefinitely.

```tallyne
# forever <TASK>
```

### While

Repeats a Task while a condition remains true.

```tallyne
# while (condition) do <TASK>
```

---

## 10. Conditionals

Tallyne uses the `?` prefix to define a **Check Task**.

A Check Task evaluates a condition and stores its result in the global Key Variable `call`.

### Syntax

```tallyne
? (operand1) <operator> (operand2)
```

### Check Operators

| Operator | Meaning | Behaviour |
|---|---|---|
| `=` | Numerical equality | Compares operands numerically |
| `!=` | Numerical inequality | Checks numerical inequality |
| `is` | Identity equality | Compares raw values, ignoring case |
| `not` | Identity inequality | Checks whether raw values differ |
| `>` | Greater than | Numerical comparison |
| `<` | Less than | Numerical comparison |
| `>=` | Greater than or equal | Numerical comparison |
| `<=` | Less than or equal | Numerical comparison |
| `is empty` | NULL check | Checks whether an operand contains NULL |

### Example: Basic Conditional

```tallyne
? (current_fuel) < "10"

# if call is true, radio "Low fuel" to IO
# else, radio "Fuel level OK" to IO
```

### Example: Routine Ladder

```tallyne
{CHECK_MOTOR_SPEED}
    ? (speed) = "0"
    # if call is true, {MOTOR_IS_STOPPED}

    ? (speed) > "50"
    # if call is true, {MOTOR_IS_FAST}

    # else, {MOTOR_IS_NORMAL}
# ***
```

The `call` variable is updated whenever a Check Task is executed.

---

## 11. Comments

Comments begin with `//`.

They are ignored during program execution and can be used to document code.

```tallyne
// This is a comment

{TASKS}
    # radio "Hello World" to IO
# ***
```

---

## 12. A Complete Example

The following program demonstrates Routine declarations, parameters, arithmetic, storage, and communication.

```tallyne
{ADDITION} with (A), (B)
    $ (A) + (B)
    # return yield
# ***

{TASKS}
    # prompt {ADDITION} with "25", "75"

    # radio "The result is:" to IO
    # radio it to IO
# ***
```

Expected output:

```text
The result is:
100
```

---

## Project Status

Tallyne is an experimental language under active development.

Its syntax, execution model, and interpreter are being developed together. Some language behaviours and implementation details remain subject to refinement.

**Tallyne — Programming, one task at a time.**
