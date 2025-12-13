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
<#>
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


## 📦  Lists (Arrays)

A **List** in Tallyne is an ordered collection of Hybrid Values, enclosed in square brackets `[]`. Lists are stored and referenced using a Variable `()`. Tallyne uses **0-based indexing** for list access (the first item is index `0`).

### 📝 List Declaration

  * **Creating a List (Literal):**

    ```tallyne
    # store ["red", "green", "blue"] in (colors)
    ```

  * **Creating an Empty List:**

    ```tallyne
    # store [] in (readings)
    ```

### 🛠️ List Management Tasks

Tallyne provides distinct tasks for reading, adding/inserting, updating, and removing list items.

| Task | Syntax | Action |
| :--- | :--- | :--- |
| **`# pull`** (Retrieve) | `# pull <index> from (list)` | Retrieves the value at the specified index. Result is stored in **`it`**. |
| **`# insert`** (Shift/Append) | `# insert <value> at <index> in (list)` | **Inserts** the value at the index, shifting all subsequent items. |
| **`# insert`** (Append Only) | `# insert <value> into (list)` | **Appends** the value to the very end of the List. |
| **`# mark`** (Update) | `# mark <value> at <index> in (list)` | **Updates** the existing value at the specified index without changing the list size or order. |
| **`# cut`** (Remove) | `# cut <index> from (list)` | Removes the item at the index. The removed item's value is stored in **`it`**. |

### 💡 Example Flow

```tallyne
{LIST_DEMO}
# store ["apple", "banana", "kiwi"] in (fruits)

// Puts "banana" into 'it'
# pull 1 from (fruits) 
# radio it to IO 

// Updates existing item at index 0
# mark "MANGO" at 0 in (fruits) 
// (fruits) is now ["MANGO", "banana", "kiwi"]

// Inserts "grape" at index 1, shifting "banana" and "kiwi"
# insert "grape" at 1 in (fruits) 
// (fruits) is now ["MANGO", "grape", "banana", "kiwi"]

// Appends "orange" to the end
# insert "orange" into (fruits) 
// (fruits) is now ["MANGO", "grape", "banana", "kiwi", "orange"]

// Removes "kiwi" (now at index 3), stores "kiwi" in 'it'
# cut 3 from (fruits) 
// (fruits) is now ["MANGO", "grape", "banana", "orange"]
<#>
```

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

| Operator | Meaning | Data Type Focus | Example | Action/Note |
| :---: | :--- | :--- | :--- | :--- |
| **`=`** | Numerical Equality (`==`) | **Num** | `? (A) = (B)` | Interpreter attempts to convert both operands to numbers before checking if they are equal. |
| **`!=`** | Numerical Inequality (`!=`) | **Num** | `? (A) != (B)` | Interpreter attempts to convert both operands to numbers before checking if they are unequal. |
| **`is`** | Identity Equality | **Non-Num / Raw String** | `? (A) is (B)` | Compares the raw, case-insensitive string content of the Hybrid Values. |
| **`not`** | Identity Inequality | **Non-Num / Raw String** | `? (A) not (B)` | Checks if the raw, case-insensitive string content of the Hybrid Values are *not* identical. |
| `>` | Greater Than | **Num** | `? (A) > (B)` | Forced Numerical magnitude check. |
| `<` | Less Than | **Num** | `? (A) < (B)` | Forced Numerical magnitude check. |
| `>=` | Greater or Equal | **Num** | `? (A) >= (B)` | Forced Numerical magnitude check. |
| `<=` | Less or Equal | **Num** | `? (A) <= (B)` | Forced Numerical magnitude check. |
| `is empty` | Checks if a variable holds no value. | N/A | `? (input) is empty` | Checks if the operand is unassigned (null/uninitialized). |

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
