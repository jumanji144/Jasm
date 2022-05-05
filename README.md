# Jasm
Java assembly like language for bytecode.

# Syntax
The syntax is quite simelar to that of other assemblers.
It also supports names for locals, labels
but most importantly it supports the entire structure of class declaration, field declaration and method declartaion
here is a sample
```jasmin
class public Test extends java/lang/Object

field public number I

method public <init>()V
    aload this
    invokespecial java/lang/Object/<init>()V
    aload this
    
    bipush 13
    putfield Test.number I
    
    return
end

method public test()V

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

# Credits

Mostly i would like to credit [Jasmin](https://github.com/davidar/jasmin) for inspriation for the syntax and outline of the project      
And also [ObjectWeb ASM](https://asm.ow2.io/) for their great java assembly library to allow everything to tie together.
