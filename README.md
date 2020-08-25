# devscript
A Java inbuild script for developing and debugging

# Implementation

Basic syntax to execute a "Hello World" script as a string inside your program:
Both input and outputs are the default System.in and System.out.

```java
Process p = new Process(true);
p.addSystemOutput();
p.addInput(System.in);
p.execute("println \"Hello World\"");
```

# Syntax

The whole syntax is based on commands. These act like functions, that means, they can return values and accept arguments.
These arguments can be consistent of: String, Object, Boolean, Null, Any, Array_Any, Dictionary and Blocks.
Example for the command println:
  - This command expects an unlimited amount of strings: println <string> ... and returns Null, that means nothing.
  > println "Hello World" "And another line";

Command names can also be shifted, like the _<string> + <string>_ command to make the code more readable:
  >  1 + 1;
  
But this command alone does not do very much. How do you use the new, returned value?
Look at this example:
  >  println (1 + 1);
  >  println 2; 
  
Commands can be combined with others with parantheses.
  println (1 + (4 / 2));

Variables:
  Variables can get declared with the "=" command.
    foo = 10;
  And you can access them with a $ sign, followed by the variable name:
    println $foo;
  There are also a few inbuild variables:
    $true, $false and $null
    
Arrays:
  Array can get declared like this:
     array = [];
  This would be an empty array. An array filled with strings would look like this:
     array = ["String1" "String2" "String3"];
    
