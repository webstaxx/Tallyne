# Tallyne 
## Overview

Tallyne is a minimal, task-based language inspired by the structure of a To-Do list. Code is written as single-line tasks under named Routines (functions), then tallied (executed) step by step.

It combines the simplicity of Procedural Programming with the modularity of Object-Oriented Programming, letting you treat each system or hardware part as its own entity.

Designed for clarity, control, and real-world logic—perfect for robotics and automation.

---

## Main Features

* `{NAME_OF_THE_LIST}` defines **Routine** (function).
* Each **task** (a statement) is prefixed with `#` under a Routine.
* Any line starting with `//` is a **comment**.
* `<#>` marks the end of the Routine.
* The `{TASKS}` Routine is executed by default (Main Method)
* Tallyne is not case sensitive, meaning `HELLO` and `hello` are treated the same.
* Anything in `""` is considered a **VALUE** parameter.
* Anything in `{}` refers to a **Routine** parameter.
* There is a special variable named `it` which stores the output of corresponding previous operations. `it` is an integral part of Tallyne's data flow

---

## Entities

Entities are similar to objects (although not exactly like the Objects you see in OOP). They are the principal dealers of data and also essential components of Tallyne's Procedure structure.

Example entities:

* `{TASKS}` - Main Routine (entry point)
* `{Routine_name}` - Any custom-defined Routine
* `IO` - Input/output
* Any connected hardware components such as:
  * `LeftMotor`
  * `IRSensor`
  * `Camera` etc..

---
## Data Flow
There is a special variable named `it` which stores the output of corresponding previous operations. `it` is an integral part of Tallyne's data flow

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

### `radio` — Set Values Without Running

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

> Using `# prompt {Routine}` without parameters results in: `Empty Prompt`.

---

## Storage

```tallyne
# create "variable_name"                  // Creates a variable (default type: VALUE)
# store "value" in (variable)              // Assign value to variable
# create and store "value" in (variable)   // Create + assign in one step
# store it in (variable)                   // Copy `it` into another variable
```

If a task's result isn't explicitly stored, it's automatically saved in `it`.

---

## Input/Output

```tallyne
# radio "Hello World" to IO
# query IO with "What is your name"    // Stores input in `it`
```

---

## Mathematical Operations

```tallyne
# add (num1), (num2), (num3)            // Sum
# subtract (num1), (num2)               // Subtract sequentially
# multiply (num1), (num2)               // Multiply sequentially
# divide (num1), (num2)                 // Quotient
# modulus (num1), (num2)                // Remainder
```

If used without explicit storage, the result is saved in `it`.

---

## Conditionals

### Single Condition

```tallyne
# if (condition) is true, <task>
# else, <another task>
```

### Routine-Based

```tallyne
# if (condition) is true, {routine_if_true}
# else, {routine_if_false}
```

### Ladder

```tallyne
# if (condition1) is true, {routine1}
# else if (condition2) is true, {routine2}
# else, {fallback_routine}
```

Only the first true condition's routine will be executed.

---

## Loops

```tallyne
# forever <TASK>                      // Infinite loop
# while (condition) do <TASK>         // Repeat until condition is false
```

---
