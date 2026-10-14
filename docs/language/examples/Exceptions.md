# Exceptions

## Java source

```java
public class ExceptionPatterns {
    public static int parseOrDefault(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
```

## JVM JASM

```jasm
.version 8
.super java/lang/Object
.class public super ExceptionPatterns {
    .method public static parseOrDefault (Ljava/lang/String;)I {
        parameters: { text },
        exceptions: { 
            { A, B, C, Ljava/lang/NumberFormatException; }
        },
        code: {
        A: 
            aload text
            invokestatic java/lang/Integer.parseInt (Ljava/lang/String;)I
        B: 
            ireturn 
        C: 
            astore exception
        D: 
            iconst_m1 
            ireturn 
        E: 
        }
    }
}
```

## Dalvik JASM

```jasm
.super java/lang/Object
.class public ExceptionPatterns {
    .method public static parseOrDefault (Ljava/lang/String;)I {
        registers: 3,
        parameters: { text },
        exceptions: { { A, B, C, java/lang/NumberFormatException } },
        code: {
        A: 
            invoke-static { v2 } java/lang/Integer.parseInt (Ljava/lang/String;)I
            move-result exception
        B: 
            return exception
        C: 
            move-exception exception
        D: 
            const v1 -1
            return v1
        E: 
        }
    }
}
```
