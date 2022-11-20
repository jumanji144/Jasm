# Jasm
[![](https://jitpack.io/v/Nowilltolife/Jasm.svg)](https://jitpack.io/#Nowilltolife/Jasm)
[![](https://img.shields.io/github/license/Nowilltolife/Jasm)](https://github.com/Nowilltolife/Jasm)


Java assembly like language for bytecode. Aimed at intergration use.

# Syntax

The syntax is quite similar to that of other assemblers.   
It has resilience against maliciously inputs
It also supports names for locals, labels
but most importantly it supports the entire structure of class declaration, field declaration and method declartaion
here is a sample
```jasmin
class public Test extends java/lang/Object

field public number I

method public <init> ()V
    aload this
    invokespecial java/lang/Object.<init> ()V
    aload this
    
    bipush 13
    putfield Test.number I
    
    return
end

method public test()V

    getstatic java/lang/System.out Ljava/io/PrintStream;
    ldc "Hello, World!"
    invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V

    getstatic java/lang/System.out Ljava/io/PrintStream;
    aload this
    getfield Test.number I
    invokevirtual java/io/PrintStream.println (I)V
    
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
The JASM project aims at integration in other projects so for an example implementation look at the [Object web implementation](https://github.com/Nowilltolife/Jasm/blob/master/src/main/java/me/darknet/assembler/compiler/impl/ASMBaseVisitor.java) which uses the `Visitor` and `MethodVisitor` model to visit the entire AST. The AST functionality is explained further in the next section.

# Parser quirks
Due to having to support the entire JVM standard, there are some challenges to overcome, namely that most names have no limitation on what characters can be used for names which arises some problems. For example, the class names can be any keyword so imagine that the class name is 'public' or 'private' or 'final', how would the parser know what is which? The short answer is it can, but very complex. An easy solution is to just add a prefix character because luckily some characters cannot be used: '.' and '/'. So the Keyword class allows for a prefix to be defined and transform `public` -> `.public` which solves this issue. Now further up there might be names that use ' ' in part of the name (still valid) but that needs to be handled by input validation. Also, one problem is that someone might try to mess with the parser and name variables after keywords like `aload aload` for this reason there needs to be a strict identifier parser which only treats instruction arguments as text and nothing else

# API
First off I need to explain the way the parser works. The parser is a recursive descent parser
that tokenizes based on whitespaces and puts them in object-oriented structures that have a
hierarchy.     
For example:
```
'method public <init> ()V'
              MethodDeclaration
    /                 |             \
   Identifier     AccessMods      Body -> End
  (descriptor)    (modifiers)
       |              |
   <init> ()V      [AccessMod]
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

The language also supports a kind of preprocessor for marcos using the `macro` keyword.    
```jasmin
class public Macros extends java/lang/Object

macro System.out
    getstatic java/lang/System.out Ljava/io/PrintStream;
end

macro println java/io/PrintStream.println (Ljava/lang/String;)V end

method public <init> ()V
    aload this
    invokespecial java/lang/Object.<init> ()V
    return
end

method public static main ([Ljava/lang/String; args)V
    System.out // converts to 'getstatic java/lang/System/out Ljava/io/PrintStream;'
    ldc "Hello, World!"
    invokevirtual println // converts to 'invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V'
    return
end
```

Macros with arguments are planned in the future but currently not a priority 

# Credits

Mostly I would like to credit [Jasmin](https://github.com/davidar/jasmin) for inspriation for the syntax and outline of the project      
And also [ObjectWeb ASM](https://asm.ow2.io/) for their great java assembly library to allow everything to tie together.

# Projects using JASM

The project is used by the [Recaf](https://github.com/Col-E/Recaf) project in the [3.X](https://github.com/Col-E/Recaf/tree/dev3) redesign branch.
