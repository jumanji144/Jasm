# Lambda: `Consumer<String>`

## Java source

```java
import java.util.function.Consumer;

public class Lambda {
    public static void main(String[] args) {
        run("Hello world!", text -> System.out.println(text));
    }

    private static void run(String text, Consumer<String> consumer) {
        consumer.accept(text);
    }
}
```

## JVM JASM

```jasm
.version 8
.inner public static final {
    name: Lookup,
    inner: java/lang/invoke/MethodHandles$Lookup,
    outer: java/lang/invoke/MethodHandles
}
.super java/lang/Object
.class public super Lambda {
    .method public static main ([Ljava/lang/String;)V {
        parameters: { args },
        code: {
        A: 
            ldc "Hello world!"
            invokedynamic accept ()Ljava/util/function/Consumer; LambdaMetafactory.metafactory { (Ljava/lang/Object;)V, { invokestatic, Lambda.lambda$main$0, (Ljava/lang/String;)V }, (Ljava/lang/String;)V }
            invokestatic Lambda.run (Ljava/lang/String;Ljava/util/function/Consumer;)V
            return 
        B: 
        }
    }

    .signature "(Ljava/lang/String;Ljava/util/function/Consumer<Ljava/lang/String;>;)V"
    .method private static run (Ljava/lang/String;Ljava/util/function/Consumer;)V {
        parameters: { text, consumer },
        code: {
        A: 
            aload consumer
            aload text
            invokeinterface java/util/function/Consumer.accept (Ljava/lang/Object;)V
            return 
        B: 
        }
    }

    .method private static synthetic lambda$main$0 (Ljava/lang/String;)V {
        parameters: { text },
        code: {
        A: 
            getstatic java/lang/System.out Ljava/io/PrintStream;
            aload text
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
.class public Lambda {
    .method static synthetic lambda$main$0 (Ljava/lang/String;)V {
        registers: 2,
        parameters: { text },
        code: {
        A: 
            sget-object v0 java/lang/System.out Ljava/io/PrintStream;
            invoke-virtual { v0, v1 } java/io/PrintStream.println (Ljava/lang/String;)V
            return-void 
        B: 
        }
    }

    .method public static main ([Ljava/lang/String;)V {
        registers: 3,
        parameters: { args },
        code: {
        A: 
            new-instance v0 LLambda$$ExternalSyntheticLambda0;
            invoke-direct { v0 } Lambda$$ExternalSyntheticLambda0.<init> ()V
            const-string v1 "Hello world!"
            invoke-static { v1, v0 } Lambda.run (Ljava/lang/String;Ljava/util/function/Consumer;)V
            return-void 
        B: 
        }
    }

    .signature "(Ljava/lang/String;Ljava/util/function/Consumer<Ljava/lang/String;>;)V"
    .method private static run (Ljava/lang/String;Ljava/util/function/Consumer;)V {
        registers: 2,
        parameters: { text, consumer },
        code: {
        A: 
            invoke-interface { v1, v0 } java/util/function/Consumer.accept (Ljava/lang/Object;)V
            return-void 
        B: 
        }
    }

}
```

D8 also emits this companion class:

```jasm
.super java/lang/Object
.implements java/util/function/Consumer
.class public final synthetic Lambda$$ExternalSyntheticLambda0 {
    .method public synthetic constructor <init> ()V {
        registers: 1,
        code: {
        A: 
            invoke-direct { v0 } java/lang/Object.<init> ()V
            return-void 
        B: 
        }
    }

    .method public final accept (Ljava/lang/Object;)V {
        registers: 2,
        code: {
        A: 
            check-cast v1 Ljava/lang/String;
            invoke-static { v1 } Lambda.lambda$main$0 (Ljava/lang/String;)V
            return-void 
        B: 
        }
    }
}
```