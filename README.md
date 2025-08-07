# Tallyne
Tallyne is an Esolang in which you write some tasks in sentences as tasks on a To-Do list and then finally tally them all (Execute).
(Esolangs are usually funny but ig this ones more serious than funny but dw I'll make a funny one sometime when I'm bored :P)

## Structure:
- `{NAME_OF_THE_LIST}` Creates a new "Routine" (much like a Class).
- Each "task" (a statement) is written in a bullet `#` under a Routine.
- Whatever not written in a bullet is considered a comment :|
- `<#>` marks the end of the Routine.
- The `{TASKS}` Routine is executed by default.
- Tallyne is not case sensitive which means "HELLO" and "hello" are treated same.
- Anything written in `""` is considered as a VALUE as parameter.
- Anything written in `{}` is refering to a Routine as parameter.
- Anything written in `<>` is for accepting a TASK as parameter.

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
- Hard - Hardware/Robotics framework
- {TASKS} - Main Routine
- Any new Routine created

### Storage:
- `# create "variable_name"` - Create a variable, its default data type will be "VALUE"
- `# store "value" in (variable)` - Assigning value to variable
- `# create and store "value" in (variable)` - Create and assign value at the same time
Whatever task when tallied, if it has a result which is not specifically passed to a Variable, it will by default get stored in a predefined storage space called "it". This helps in the continuity of tasks and refer to previous tasks from later tasks.
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

### If-Else:
Since tasks are single lined, for the ease of Interpreting the code, if.. else if.. Tasks have to be separated into different Routines.
- `# if (condition) is true, <TASK>` - Checks if a condition is true and performs a TASK depending on it
- `# if (condition) is true, <check {Routine_true}> else, <check {Routine_false}>` - Checks if a condition is true and calls different Routines depending on it
- `# if (condition) is false, <check {Routine_true}> else, <check {Routine_false}>` - Checks if a condition is false and calls different Routines depending on it
Performing an If..Else If can be tricky, you have to put and if TASK in the else part.
- `# if (condition) is true, <check {Routine_true}> else, <if (condition) is true, <TASK> else, <ANOTHERTASK>` - Checks if a multiple conditions are true

### Loops:
- `# forever <TASK>` - Repeat some task forever
- `# while (condition) do <TASK>` - Repeat Task until the condition turns false
