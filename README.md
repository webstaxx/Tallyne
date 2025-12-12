# Tallyne 

Tallyne is a minimal, task-based language inspired by the structure of a To-Do list. Code is written as single-line tasks under named Routines (functions), then tallied (executed) step by step.

It combines the simplicity of Procedural Programming with the modularity of Object-Oriented Programming, letting you treat each system or hardware part as its own entity.

Designed for clarity, control, and real-world logic—perfect for robotics and automation.

---

## Main Features

* `{NAME}` defines **Routine** (function).
* Each **task** (a statement) is prefixed with `#` under a Routine.
* Any line starting with `//` is a **comment**.
* `<#>` marks the end of the Routine.
* The `{TASKS}` Routine is executed by default (Main Method)
* Tallyne is not case sensitive, meaning `HELLO` and `hello` are treated the same.
* All VALUES are referred within `""`
* All ROUTUNES are referred within `{}`
* All VARIABLES are referred within `()`
* There specific variables in tallyne called key-variables, as they behave like keywords built into tallyne. They can be referred without using parentheses. They are an integral part of Tallyne's data flow

---

## Entities

Entities are similar to objects (although not exactly like the Objects you see in OOP). They are the principal dealers of data and also essential components of Tallyne's Procedure structure.

Example entities:

* `{TASKS}` - Main Routine (entry point)
* `{Routine_name}` - Any custom-defined Routine
* `IO` - Input/output  (SYSTEM ENTITY)

Any connected hardware components can be treated as Entities with the requied plugins, eg:
* `LeftMotor`
* `IRSensor`
* `Camera` etc..

Entities can be classified into two based on their State:
* Alive Entities - They are active the entire time while a program runs. Usually System Entities.
* Idle Entities - They are not active unless that entity is called for. User-defined Entities usually fall in this cateory.

---
## Data Flow
Introducing first Key-Variable, **it**:
`it` stores the output of corresponding previous operations. This is an essential feature of data flow.

Tallyne passes information between routines and entities using the following:
- `# radio <information> to <reciever>` - Broadcasts data to an entity (It has a lot of uses)
- `# query <someone> with <query>` - Requests some entity for data.

Eg:
```tallyne
// Code to input name and then display it back
# query IO with "What is your name?"
# radio it to IO
```

---

## Functions / Methods

### Declaring Functions (Routines):

Rouines are declared with the Header `{NAME_OF_THE_ROUTINE}`.
Tasks within the routine are written line by line under the Header.
`<#>` marks the end of the routine.

That is the simplest way to create a Routine, but for it to become a proper Function, it must take in values and output values.
A routine can take in parameters y modifying the Header to accomodate for them. This is done by using `with` keyword:

```tallyne
{ADDITION} with "A", "B", "C"
// (A), (B), and (C) have become parameters of the Routine and can be used inside the routine as normal variables
? (A) + (B) + (C)
# store yield in it
# radio "Addition Complete and reurned value to it" to IO
```


### 

Assigns values to a Routine without executing it. Useful for preparing state before calling with `tally`.

```tallyne
# radio "value1", "value2" to {Routine}
```

There are two ways to invoke a Routine in Tallyne:

### `tally` — Run With Existing Values

Executes a Routine with whatever values it already has (set earlier using `radio` or defaults).

```tallyne
# tally {Routine}
```

### `prompt` — Pass Values and Run Immediately

Passes values to a Routine **and** executes it immediately. Cannot be used without parameters.

```tallyne
# prompt {Routine} with "value1", "value2"  // Executes right away
```

> Using `# prompt {Routine}` without parameters results in error: `Empty Prompt`.

---

## Storage
Tallyne has a simple variable system.
```tallyne
# store "value" in (variable)              // Declares and initializes a variable with "value"
# store it in (variable)                   // Updates variable's value to the data in 'it'
```
If a task's result isn't explicitly stored, it's automatically saved in a temporary multipurpose variable `it`. It is a core feature of Tallyne.

---

## Input/Output

```tallyne
# radio "Hello World" to IO
# query IO with "What is your name"    // Stores input in `it`
```
> Note: `prompt` is not required for output as IO is an ALIVE entity and will execute by `radio` alone. The two could be used interchangeably in the case of ALIVE entities.

## Mathematical Operations

Tallyne has built in SIMPLE commands to perform mathematical Operations on 2 variables.

```tallyne
# add (num1), (num2)                    // Sum
# subtract (num1), (num2)               // Subtract sequentially
# multiply (num1), (num2)               // Multiply sequentially
# divide (num1), (num2)                 // Quotient
# modulus (num1), (num2)                // Remainder
```
The reselt will be saved in `it`.

Tallyne has a more abstract way of handling arithmetic, which will be used for more complicated tasks:
Instead of the normal TASK prefix `#`, a prefix `$` is used to specify an Arithmetic Task. It will compute a Mathematical Expression by following PEMDAS and stores the result in the key-variable `yield`, similar to the variable `it` but only for mathematical purposes.

Example code:
```tallyne
$ 4*4 + (12+3) * 10             // Arithmetic Expression, follows PEMDAS
# store yield in (sum)          // Stores the result of the Expression in a variable
# radio (sum) to IO             // Outputs 166
```
---

## Loops

```tallyne
# forever <TASK>                      // Infinite loop
# while (condition) do <TASK>         // Repeat until condition is false
```

---

## Conditionals

Tallyne uses a distinct prefix, `?`, for **Check Tasks** (conditionals).

A Check Task evaluates a single condition and immediately stores the boolean result (`TRUE` or `FALSE`) into the dedicated system variable, **`call`**. This variable is then used by the `# if` Action Tasks to control the program flow.

### Check Task Syntax

A Check Task follows the pattern:

```tallyne
? (operand1) <operator> (operand2)
```

### Check Operators

These are the valid operators used in a Check Task:

| Operator | Meaning | Example |
| :---: | :--- | :--- |
| `=` | Equality (`==`) | `? (a) = (b)` |
| `!=` | Not Equal (`!=`) | `? (a) != (b)` |
| `>` | Greater Than (`>`) | `? (a) > (b)` |
| `<` | Less Than (`<`) | `? (a) < (b)` |
| `>=` | Greater or Equal (`>=`) | `? (a) >= (b)` |
| `<=` | Less or Equal (`<=`) | `? (a) <= (b)` |
| `is empty` | Checks if a variable holds no value (null/unassigned). | `? (user_input) is empty` |

-----

### Task-Based Control Flow

The `call` variable is then used in standard `# if` structures to determine which routine to execute or which task to run.

#### Single Condition

```tallyne
? (current_fuel) < "10"
# if call is true, radio "Low fuel" to IO
# else, radio "Fuel level OK" to IO
```

#### Routine Ladder Example

```tallyne
{CHECK_MOTOR_SPEED}
? (speed) = "0"
# if call is true, {MOTOR_IS_STOPPED}

? (speed) > "50"
# if call is true, {MOTOR_IS_FAST}

# else, {MOTOR_IS_NORMAL}
<#>
```

-----

###  Key System Variables Review

| Variable | Prefix/Used With | Purpose | 
| :---: | :--- | :--- | 
| `it` | `#` (Action Tasks) | Stores the output or temporary value of the most recent Action Task. | 
| `yield` | `$` (Math Tasks) | Stores the numerical result of the most recent Arithmetic Task | 
| `call` | `?` (Check Tasks) | Stores the boolean result (`TRUE` / `FALSE`) of the most recent Check Task. |

---
