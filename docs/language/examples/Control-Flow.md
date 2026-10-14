# Loops and branches

## Java source

```java
public class ControlFlow {
    public static int sumOddSquaresBelow(int limit) {
        int sum = 0;
        for (int value = 0; value < limit; value++) {
            if ((value & 1) == 0) {
                continue;
            }
            if (value > 99) {
                break;
            }
            sum += value * value;
        }
        return sum;
    }
}
```

## JVM JASM

```jasm
.version 8
.super java/lang/Object
.class public super ControlFlow {
    .method public static sumOddSquaresBelow (I)I {
        parameters: { limit },
        code: {
        A: 
            iconst_0 
            istore sum
        B: 
            iconst_0 
            istore value
        C: 
            iload value
            iload limit
            if_icmpge J
        D: 
            iload value
            iconst_1 
            iand 
            ifne F
        E: 
            goto I
        F: 
            iload value
            bipush 99
            if_icmple H
        G: 
            goto J
        H: 
            iload sum
            iload value
            iload value
            imul 
            iadd 
            istore sum
        I: 
            iinc value 1
            goto C
        J: 
            iload sum
            ireturn 
        K: 
        }
    }
}
```

## Dalvik JASM

```jasm
.super java/lang/Object
.class public ControlFlow {
    .method public static sumOddSquaresBelow (I)I {
        registers: 4,
        parameters: { limit },
        code: {
        A: 
            const sum 0
        B: 
            const value 0
        C: 
            if-ge value v3 J
        D: 
            and-int/lit8 v2 value 1
            if-nez v2 F
        E: 
            goto I
        F: 
            const v2 99
            if-le value v2 H
        G: 
            goto J
        H: 
            mul-int v2 value value
            add-int/2addr sum v2
        I: 
            add-int/lit8 value value 1
            goto C
        J: 
            return sum
        K: 
        }
    }
}
```
