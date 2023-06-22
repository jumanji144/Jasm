[![forthebadge](https://forthebadge.com/images/badges/contains-tasty-spaghetti-code.svg)](https://forthebadge.com)
# Jasm rewrite

This is a rewrite branch of JASM using a new syntax, and error recovery
mechanism.

## New syntax approach
Instead of using the classic `end` keyword, i have chosen to adopt a operator driven design:
```jasmin
.class public HelloWorld {
    .method public static main([Ljava/lang/String;)V {
        parameters: [args]
        code: {
            getstatic HelloWorld.out Ljava/io/PrintStream;
            ldc "Hello World!"
            invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
            return
        }
    }
}
```