# Lambda: `Consumer<String>`

Source Java:

```java
import java.util.function.*;
    
public class Lambda {
    public static void main(String[] args) {
        run("Hello world!", text -> System.out.println(text));
    }
    
    private static void run(String text, Consumer<String> consumer) {
        consumer.accept(text);
    }
}
```

JVM Jasm:

```
.version 8
.inner public static final {
    name: Lookup,
    inner: java/lang/invoke/MethodHandles$Lookup,
    outer: java/lang/invoke/MethodHandles
}
.sourcefile "Lambda.java"
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