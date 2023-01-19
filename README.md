# MiniJava Compiler
This project performs semantic analysis for the language "MiniJava" and converts it into the intermediate representation used by the LLVM compiler project using JTB and JavaCC.

## Run 
```
make
java Main [file1] [file2] ... [fileN]
```

## MiniJava Definition
MiniJava is a subset of Java. MiniJava is designed so that its programs can be compiled by a full Java compiler like javac.

* MiniJava is fully object-oriented, like Java. It does not allow global functions, only classes, fields and methods. The basic types are int, boolean, int [] which is an array of int, and boolean [] which is an array of boolean. You can build classes that contain fields of these basic types or of other classes. Classes contain methods with arguments of basic or class types, etc.

* MiniJava supports single inheritance but not interfaces. It does not support function overloading, which means that each method name must be unique. In addition, all methods are inherently polymorphic (i.e., “virtual” in C++ terminology). This means that foo can be defined in a subclass if it has the same return type and argument types (ordered) as in the parent, but it is an error if it exists with other argument types or return type in the parent. Also all methods must have a return type--there are no void methods. Fields in the base and derived class are allowed to have the same names, and are essentially different fields.

* All MiniJava methods are “public” and all fields “protected”. A class method cannot access fields of another class, with the exception of its superclasses. Methods are visible, however. A class's own methods can be called via “this”. E.g., this.foo(5) calls the object's own foo method, a.foo(5) calls the foo method of object a. Local variables are defined only at the beginning of a method. A name cannot be repeated in local variables (of the same method) and cannot be repeated in fields (of the same class). A local variable x shadows a field x of the surrounding class.
In MiniJava, constructors and destructors are not defined. The new operator calls a default void constructor. In addition, there are no inner classes and there are no static methods or fields. By exception, the pseudo-static method “main” is handled specially in the grammar. A MiniJava program is a file that begins with a special class that contains the main method and specific arguments that are not used. The special class has no fields. After it, other classes are defined that can have fields and methods.

* Notably, an A class can contain a field of type B, where B is defined later in the file. But when we have "class B extends A”, A must be defined before B. As you'll notice in the grammar, MiniJava offers very simple ways to construct expressions and only allows < comparisons. There are no lists of operations, e.g., 1 + 2 + 3, but a method call on one object may be used as an argument for another method call. In terms of logical operators, MiniJava allows the logical and ("&&") and the logical not ("!"). For int and boolean arrays, the assignment and [] operators are allowed, as well as the a.length expression, which returns the size of array a. We have “while” and “if” code blocks. The latter are always followed by an “else”. Finally, the assignment "A a = new B();" when B extends A is correct, and the same applies when a method expects a parameter of type A and a B instance is given instead.

## Semantic Analysis
Two visitors that implement the GJDepthFirst visitor perform semantic Analysis (FirstVisitor.java and SecondVisitor.java). 

A symbol table (SymbolTable.java) keeps information about the variables, methods and classes, like inheritance, what belongs where etc. 

The first visitor fills the symbol table and also stores some useful data for every class such as the names and the offsets of every field and method this class contains. For MiniJava we have only three types of fields (int, boolean and pointers). Ints are stored in 4 bytes, booleans in 1 byte and pointers in 8 bytes (we consider functions and arrays as pointers). This will be useful in generating intermediate LLVM code. These are stored in Offset class (Offset.java).

The second visitor checks if the types are compatible and finds more complicated semantic errors which the first visitor doesn't, using the stored information in the symbol table.

## Generating intermediate LLVM code
For this part, I implemented the LLVMVisitor (LLVMVisitor.java) which uses the symbol table created before so that it can compile regarding to the type. The offsets calculated before are used to produce the v-table.
> The v-table is a table of function pointers, pointed at by the first 8 bytes of an object. The v-table defines an address for each dynamic function the object supports.
