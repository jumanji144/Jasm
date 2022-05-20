# Jasm
Java assembly like language for bytecode.

# Syntax

```
!IMPORTANT! The syntax is heavy in WIP so below information or examples may or may not change
To be certain if the syntax is correct come back when this warning is gone
```

The syntax is quite similar to that of other assemblers.   
It has some resilience against maliciously user-defined variables.   
It also supports names for locals, labels
but most importantly it supports the entire structure of class declaration, field declaration and method declartaion
here is a sample
```jasmin
class .public Test extends java/lang/Object

field .public number I

method .public <init>()V
    aload this
    invokespecial java/lang/Object/<init>()V
    aload this
    
    bipush 13
    putfield Test.number I
    
    return
end

method .public test()V

    getstatic java/lang/System.out Ljava/io/PrintStream;
    ldc "Hello, World!"
    invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

    getstatic java/lang/System.out Ljava/io/PrintStream;
    aload this
    getfield Test.number I
    invokevirtual java/io/PrintStream/println(I)V
    
    return
end
```
which in java code would be
```
public class Test {
    public int number = 13;

    public void test() {
        System.out.println("Hello, World!");
        System.out.println(this.number);
    }
}
```

# Usage
To use Jasm you can either use the CLI to compile .ja files to .class files or you can directly
use the Jasm parser in your project to compile .ja files to anything you want using the API.

# API
First off I need to explain the way the parser works. The parser is a recursive descent parser
that tokenizes based on whitespaces and puts them in object-oriented structures that have a
hierarchy.     
For example:
```
'method .public <init>()V'
              MethodDeclaration
    /                 |             \
   Identifier     AccessMods      Body -> End
  (descriptor)    (modifiers)
       |              |
   <init>()V      [AccessMod]
                      |
                   Identifier
                    (name)
                      |
                    public
```
So an applicable API would be a visitor that can visit the AST and do something with it.
The `Visitor` class is the base class, it visits all declarations and top-level statements.
And for method bodies there is a `MethodVisitor` for each in body instruction.
you retrive the `MethodVisitor` by calling `visitMethod` in the `Visitor` class.
There is also the `Transformer` class that applies the AST to the visitor.
# Macros
The language also supports a kind of preprocessor for marcos using the `macro` keyword      
Example

```jasmin
class .public Macros extends java/lang/Object

macro System.out
    getstatic java/lang/System/out Ljava/io/PrintStream;
end

macro println java/io/PrintStream/println(Ljava/lang/String;)V end

method .public <init>()V
    aload this
    invokespecial java/lang/Object/<init>()V
    return
end

method .public .static main([Ljava/lang/String;)V
    System.out # converts to 'getstatic java/lang/System/out Ljava/io/PrintStream;'
    ldc "Hello, World!"
    invokevirtual println # converts to 'invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V'
    return
end
```

Macros with arguments are planned in the future but currently not a priority 
# Credits

Mostly I would like to credit [Jasmin](https://github.com/davidar/jasmin) for inspriation for the syntax and outline of the project      
And also [ObjectWeb ASM](https://asm.ow2.io/) for their great java assembly library to allow everything to tie together.
