# Hello World

Source Java:

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello!");
    }
}

```

JVM Jasm:

```
.version 8
.sourcefile "HelloWorld.java"
.super java/lang/Object
.class public super HelloWorld {
    .method public static main ([Ljava/lang/String;)V {
        parameters: { args },
        code: {
        A: 
            getstatic java/lang/System.out Ljava/io/PrintStream;
            ldc "Hello!"
            invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
            return 
        B: 
        }
    }
}
```