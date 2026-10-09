# Collections and enhanced `for`

## Java source

```java
import java.util.List;

public class CollectionPatterns {
    public static int totalLength(List<String> names) {
        int total = 0;
        for (String name : names) {
            if (name != null) {
                total += name.length();
            }
        }
        return total;
    }
}
```

## JVM JASM

```jasm
.version 8
.super java/lang/Object
.class public super CollectionPatterns {
    .signature "(Ljava/util/List<Ljava/lang/String;>;)I"
    .method public static totalLength (Ljava/util/List;)I {
        parameters: { names },
        code: {
        A: 
            iconst_0 
            istore total
        B: 
            aload names
            invokeinterface java/util/List.iterator ()Ljava/util/Iterator;
            astore v2
        C: 
            aload v2
            invokeinterface java/util/Iterator.hasNext ()Z
            ifeq G
            aload v2
            invokeinterface java/util/Iterator.next ()Ljava/lang/Object;
            checkcast java/lang/String
            astore name
        D: 
            aload name
            ifnull F
        E: 
            iload total
            aload name
            invokevirtual java/lang/String.length ()I
            iadd 
            istore total
        F: 
            goto C
        G: 
            iload total
            ireturn 
        H: 
        }
    }
}
```

## Dalvik JASM

```jasm
.super java/lang/Object
.class public CollectionPatterns {
    .signature "(Ljava/util/List<Ljava/lang/String;>;)I"
    .method public static totalLength (Ljava/util/List;)I {
        registers: 5,
        parameters: { names },
        code: {
        A: 
            const total 0
        B: 
            invoke-interface { v4 } java/util/List.iterator ()Ljava/util/Iterator;
            move-result-object v1
        C: 
            invoke-interface { v1 } java/util/Iterator.hasNext ()Z
            move-result name
            if-eqz name G
            invoke-interface { v1 } java/util/Iterator.next ()Ljava/lang/Object;
            move-result-object name
            check-cast name Ljava/lang/String;
        D: 
            if-eqz name F
        E: 
            invoke-virtual { name } java/lang/String.length ()I
            move-result v3
            add-int/2addr total v3
        F: 
            goto C
        G: 
            return total
        H: 
        }
    }
}
```
