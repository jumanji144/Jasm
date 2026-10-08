# Switch statements

## Linear integer switch, 0-3

Source Java:

```java
static void linearIntSwitch(int i) {
    switch (i) {
        case 0:
            zero();
            break;
        case 1:
            one();
            break;
        case 2:
            two();
            break;
        case 3:
            three();
            break;
        default:
            break;
    }
}
```

JVM Jasm:

```
.method static linearIntSwitch (I)V {
    parameters: { i },
    code: {
    A: 
        iload i
        tableswitch {
            min: 0,
            max: 3,
            cases: { B, C, D, E },
            default: F
        }
    B: 
        invokestatic Switches.zero ()V
        goto F
    C: 
        invokestatic Switches.one ()V
        goto F
    D: 
        invokestatic Switches.two ()V
        goto F
    E: 
        invokestatic Switches.three ()V
        goto F
    F: 
        return 
    G: 
    }
}
```

## Sparse integer switch, 0, 5, 10

Source Java:

```java
static void sparseIntSwitch(int i) {
    switch (i) {
        case 0:
            zero();
            break;
        case 5:
            five();
            break;
        case 10:
            ten();
            break;
        default:
            break;
    }
}
```

JVM Jasm:

```
.method static sparseIntSwitch (I)V {
    parameters: { i },
    code: {
    A: 
        iload i
        lookupswitch {
            0: B,
            5: C,
            10: D,
            default: E
        }
    B: 
        invokestatic Switches.zero ()V
        goto E
    C: 
        invokestatic Switches.five ()V
        goto E
    D: 
        invokestatic Switches.ten ()V
        goto E
    E: 
        return 
    F: 
    }
}
```

## String switch

Java source:

```java

```

JVM Jasm:

```
.method static stringSwitch (Ljava/lang/String;)V {
    parameters: { s },
    code: {
    A: 
        aload s
        astore sCopy
        iconst_m1 
        istore mappedIndex
        aload sCopy
        invokevirtual java/lang/String.hashCode ()I
        lookupswitch {
            43: B,
            45: C,
            default: D
        }
    B: 
        aload sCopy
        ldc "+"
        invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
        ifeq D
        iconst_0 
        istore mappedIndex
        goto D
    C: 
        aload sCopy
        ldc "-"
        invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
        ifeq D
        iconst_1 
        istore mappedIndex
    D: 
        lookupswitch {
            0: E,
            1: G,
            default: I
        }
    E: 
        invokestatic Switches.plus ()V
    F: 
        goto J
    G: 
        invokestatic Switches.minus ()V
    H: 
        goto J
    I: 
        new java/lang/IllegalStateException
        dup 
        invokespecial java/lang/IllegalStateException.<init> ()V
        athrow 
    J: 
        return 
    K: 
    }
}
```