# Primitive arrays

## Java source

```java
public class ArrayPatterns {
    public static int sum(int[] values) {
        int total = 0;
        for (int index = 0; index < values.length; index++) {
            total += values[index];
        }
        return total;
    }

    public static void fill(int[] values, int value) {
        for (int index = 0; index < values.length; index++) {
            values[index] = value;
        }
    }
}
```

## JVM JASM

```jasm
.version 8
.super java/lang/Object
.class public super ArrayPatterns {
    .method public static sum ([I)I {
        parameters: { values },
        code: {
        A: 
            iconst_0 
            istore total
        B: 
            iconst_0 
            istore index
        C: 
            iload index
            aload values
            arraylength 
            if_icmpge F
        D: 
            iload total
            aload values
            iload index
            iaload 
            iadd 
            istore total
        E: 
            iinc index 1
            goto C
        F: 
            iload total
            ireturn 
        G: 
        }
    }

    .method public static fill ([II)V {
        parameters: { values, value },
        code: {
        A: 
            iconst_0 
            istore index
        B: 
            iload index
            aload values
            arraylength 
            if_icmpge E
        C: 
            aload values
            iload index
            iload value
            iastore 
        D: 
            iinc index 1
            goto B
        E: 
            return 
        F: 
        }
    }
}
```

## Dalvik JASM

```jasm
.super java/lang/Object
.class public ArrayPatterns {
    .method public static fill ([II)V {
        registers: 4,
        parameters: { values, value },
        code: {
        A: 
            const index 0
        B: 
            array-length v1 v2
            if-ge index v1 E
        C: 
            aput v3 v2 index
        D: 
            add-int/lit8 index index 1
            goto B
        E: 
            return-void 
        F: 
        }
    }

    .method public static sum ([I)I {
        registers: 4,
        parameters: { values },
        code: {
        A: 
            const total 0
        B: 
            const index 0
        C: 
            array-length v2 v3
            if-ge index v2 F
        D: 
            aget v2 v3 index
            add-int/2addr total v2
        E: 
            add-int/lit8 index index 1
            goto C
        F: 
            return total
        G: 
        }
    }
}
```
