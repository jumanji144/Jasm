# Hello World

## Java source

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello!");
    }
}
```

## JVM JASM

```jasm
.version 8
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

## Dalvik JASM

```jasm
.super java/lang/Object
.class public HelloWorld {
    .method public static main ([Ljava/lang/String;)V {
        registers: 3,
        parameters: { args },
        code: {
        A: 
            sget-object v0 java/lang/System.out Ljava/io/PrintStream;
            const-string v1 "Hello!"
            invoke-virtual { v0, v1 } java/io/PrintStream.println (Ljava/lang/String;)V
            return-void 
        B: 
        }
    }
}
```
