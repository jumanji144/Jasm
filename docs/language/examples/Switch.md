# Switch statements

## Java source

```java
public class SwitchPatterns {
    public static int dense(int value) {
        switch (value) {
            case 0:
                return 10;
            case 1:
                return 20;
            case 2:
                return 30;
            case 3:
                return 40;
            default:
                return -1;
        }
    }

    public static int sparse(int value) {
        switch (value) {
            case -10:
                return 1;
            case 5:
                return 2;
            case 100:
                return 3;
            default:
                return 0;
        }
    }

    public static int stringCode(String value) {
        switch (value) {
            case "add":
                return 1;
            case "remove":
                return -1;
            default:
                return 0;
        }
    }
}
```

## JVM JASM

```jasm
.version 8
.super java/lang/Object
.class public super SwitchPatterns {
    .method public static dense (I)I {
        parameters: { value },
        code: {
        A: 
            iload value
            tableswitch {
                min: 0,
                max: 3,
                cases: { B, C, D, E },
                default: F
            }
        B: 
            bipush 10
            ireturn 
        C: 
            bipush 20
            ireturn 
        D: 
            bipush 30
            ireturn 
        E: 
            bipush 40
            ireturn 
        F: 
            iconst_m1 
            ireturn 
        G: 
        }
    }

    .method public static sparse (I)I {
        parameters: { value },
        code: {
        A: 
            iload value
            lookupswitch {
                -10: B,
                5: C,
                100: D,
                default: E
            }
        B: 
            iconst_1 
            ireturn 
        C: 
            iconst_2 
            ireturn 
        D: 
            iconst_3 
            ireturn 
        E: 
            iconst_0 
            ireturn 
        F: 
        }
    }

    .method public static stringCode (Ljava/lang/String;)I {
        parameters: { value },
        code: {
        A: 
            aload value
            astore v1
            iconst_m1 
            istore i2
            aload v1
            invokevirtual java/lang/String.hashCode ()I
            lookupswitch {
                -934610812: C,
                96417: B,
                default: D
            }
        B: 
            aload v1
            ldc "add"
            invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
            ifeq D
            iconst_0 
            istore i2
            goto D
        C: 
            aload v1
            ldc "remove"
            invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
            ifeq D
            iconst_1 
            istore i2
        D: 
            iload i2
            lookupswitch {
                0: E,
                1: F,
                default: G
            }
        E: 
            iconst_1 
            ireturn 
        F: 
            iconst_m1 
            ireturn 
        G: 
            iconst_0 
            ireturn 
        H: 
        }
    }
}
```

## Dalvik JASM

```jasm
.super java/lang/Object
.class public SwitchPatterns {
    .method public static dense (I)I {
        registers: 2,
        parameters: { value },
        code: {
        A: 
            packed-switch v1 {
                first: 0,
                targets: { F, E, D, C }
            }
        B: 
            const v0 -1
            return v0
        C: 
            const v0 40
            return v0
        D: 
            const v0 30
            return v0
        E: 
            const v0 20
            return v0
        F: 
            const v0 10
            return v0
            nop 
        G: 
        }
    }

    .method public static sparse (I)I {
        registers: 2,
        parameters: { value },
        code: {
        A: 
            sparse-switch v1 {
                -10: E,
                5: D,
                100: C
            }
        B: 
            const v0 0
            return v0
        C: 
            const v0 3
            return v0
        D: 
            const v0 2
            return v0
        E: 
            const v0 1
            return v0
            nop 
        F: 
        }
    }

    .method public static stringCode (Ljava/lang/String;)I {
        registers: 5,
        parameters: { value },
        code: {
        A: 
            invoke-virtual { v4 } java/lang/String.hashCode ()I
            move-result v0
            const v1 0
            const v2 1
            const v3 -1
            sparse-switch v0 {
                -934610812: D,
                96417: C
            }
        B: 
            goto E
        C: 
            const-string v0 "add"
            invoke-virtual { v4, v0 } java/lang/String.equals (Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0 B
            move v0 v1
            goto F
        D: 
            const-string v0 "remove"
            invoke-virtual { v4, v0 } java/lang/String.equals (Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0 B
            move v0 v2
            goto F
        E: 
            move v0 v3
        F: 
            packed-switch v0 {
                first: 0,
                targets: { I, H }
            }
        G: 
            return v1
        H: 
            return v3
        I: 
            return v2
        J: 
        }
    }
}
```
