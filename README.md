# Tallyne

Tallyne is a minimal, task-based language inspired by the structure of a To-Do list. Code is written as single-line tasks under named Routines, then tallied (executed) step by step.

It combines the simplicity of Procedural Programming with the modularity of Object-Oriented Programming, letting you treat each system or hardware part as its own entity.

Designed for clarity, control, and real-world logic—perfect for robotics and automation.

## Structure:
- `{NAME_OF_THE_LIST}` Creates a new "Routine" (much like a Class).
- Each "task" (a statement) is prefixed with a bullet `#` under a Routine.
- Any line not prefixed with `#` is treated as a comment.
- `<#>` marks the end of the Routine.
- The `{TASKS}` Routine is executed by default.
- Tallyne is not case sensitive which means "HELLO" and "hello" are treated same.
- Anything written in `""` is considered as a VALUE as parameter.
- Anything written in `{}` is refering to a Routine as parameter.

## Code:
### Entity (Class/Function/Method):
- `{TASKS}` - Creating the main Routine (Entity)
- `{Routine_name}` - Creating a Routine
- `# radio <info> to {Routine}` - Pass some information to a Routine as Attribute
- `# tally {Routine}` - Executing a Routine (Other than the main "TASKS" Routine)

### Info Exchange:
These command are used for information exchange with Entities
- `# radio <information> to <reciever>` - A special command that communicates something to an entity (It has a lot of uses)
- `# prompt <someone> with <information>` - A special command that requests some entity to radio them some information
  
### Example Entity:
- IO - The input-output framework
- Web - The Internet surfing framework
- Screen - GUI Framework
- {TASKS} - Main Routine
- Any custom-defined Routine
- Any connected hardware component (e.g., LeftMotor, IRSensor, Camera, UltrasonicSensor, etc.)

### Storage:
- `# create "variable_name"` - Create a variable, its default data type will be "VALUE"
- `# store "value" in (variable)` - Assigning value to variable
- `# create and store "value" in (variable)` - Create and assign value at the same time

When a task is tallied, and its result isn’t explicitly stored, it is automatically saved to a default variable called it.
- `# store "whatever" in it` - Forcefully stores something into "it"
- `# store it in (variable)` - Store "it" into another variable

### Input/Output:
- `# radio "Hello World" to IO` - Prints "Hello World" to the Terminal
- `# prompt IO with "What is your name"` - Asks for input from the user. The input will get stored into "it"

### Mathematical:
- `# add (number1) and (number2)` - Adds two numbers 1 and 2 (Any number of numbers could be added by separating them with `and`)
- `# subtract (number1) and (number2)` - Subtracts second number from first number (Any number of numbers could be subtracted from the previous by separating them with `and`)
- `# multiply (number1) and (number2)` - Multiplies two numbers 1 and 2 (Any number of numbers could be multiplied by separating them with `and`)
- `# divide (number1) and (number2)` - Divides first number by second number and returns the quotient
- `# modulus (number1) and (number2)` - Divides first number by second number and returns the remainder.

Any of these performed independently without being stored into any specific variable, will get stored to "it"

### If-Else Logic

Tallyne handles conditionals in a two-part structure. 

#### Tallies different tasks based on the Conditon:
- `# if (condition) is true, <task>`
- `# else, <another task>`

#### Tallies different Routines based on the Conditon:
- `# if (condition) is true, {routine_if_true}`
- `# else, {routine_if_false}`

#### If-Else If-Else Ladder
Use chained conditionals to handle multiple cases:
- `# if (condition1) is true, {routine1}`
- `# else if (condition2) is true, {routine2}`
- `# else, {fallback_routine}`

Only the first true condition's routine will be tallied.  
All remaining conditions will be skipped once a match is found.

`if` may or may not by followed by `else`.

### Loops:
- `# forever <TASK>` - Repeat some task forever
- `# while (condition) do <TASK>` - Repeat Task until the condition turns false
