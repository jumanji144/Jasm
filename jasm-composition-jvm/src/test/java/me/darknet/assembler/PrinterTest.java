package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Nullable
public class PrinterTest {

    private int test = 15;
    private final static int test2 = 15;
    private static final String test3 = "Hello, world!";

    @interface TestAnnotation {
        String value();

        String[] values();

        int number();

        NotNull notNull();
    }

    @TestAnnotation(
            value = "Hello, world!", values = { "Hello, world!",
                    "Hello, world!" }, number = 15, notNull = @NotNull("Hello, world!")
    )
    public static final synchronized void test(String test) {
        System.out.println("Hello, world!");
        switch (test) {
            case "test": {
                System.out.println("Hello, world!");
                break;
            }
            case "sg": {
                System.out.println("\u8153\u8157\u8135\u1183\\u1128");
                break;
            }
            default: {
                System.out.println("諯你好！");
                break;
            }
        }
        int amount = 15;
        switch (amount) {
            case 15: {
                System.out.println("Hello, world!");
                break;
            }
            case 16: {
                System.out.println("Hello, word!");
                break;
            }
            default: {
                System.out.println("Hello, wod!");
                break;
            }
        }

        String a = "Hello, world!";
        char[] b = a.toCharArray();

        int[] c = new int[10];
        Object d = c;
        if (!(d instanceof char[])) {
            Arrays.stream(c).forEach(System.out::println);
        }

        int e = 0;
        long j = 16;
        double a1 = 0.0;
        int k = 1757;
        int o = 15;
        int p = 2752387;

        int f = e + (int) j;
        double g = a1 - (double) k;
        int h = o * p;
        long i = p / o;
        int l = p % o;
        int m = p << 2;
        int n = p >> 2;
    }

    public static void main(String[] args) throws IOException {
        InputStream thisClass = PrinterTest.class.getResourceAsStream("/me/darknet/assembler/PrinterTest.class");
        PrintContext<?> ctx = new PrintContext<>("    ");
        JvmClassPrinter printer = new JvmClassPrinter(thisClass);
        printer.print(ctx);
        String output = ctx.toString();
        System.out.println(output);
        parseString(output, (result) -> {
            return;
        });
    }

    public static void parseString(String input, Consumer<Result<List<ASTElement>>> consumer) {
        DeclarationParser parser = new DeclarationParser();
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokens = tokenizer.tokenize("<stdin>", input);
        Assertions.assertNotNull(tokens);
        Assertions.assertFalse(tokens.isEmpty());
        Result<List<ASTElement>> result = parser.parseAny(tokens);
        if (result.hasErr()) {
            for (Error error : result.errors()) {
                Location location = error.getLocation();
                System.err.printf(
                        "%s:%d:%d: %s%n", location.getSource(), location.getLine(), location.getColumn(),
                        error.getMessage()
                );
                Throwable trace = new Throwable();
                trace.setStackTrace(error.getInCodeSource());
                trace.printStackTrace();
            }
            Assertions.fail();
        }
        ASTProcessor processor = new ASTProcessor(BytecodeFormat.DEFAULT);
        result = processor.processAST(result.get());
        if (result.hasErr()) {
            for (Error error : result.errors()) {
                Location location = error.getLocation();
                System.err.printf(
                        "%s:%d:%d: %s%n", location.getSource(), location.getLine(), location.getColumn(),
                        error.getMessage()
                );
                Throwable trace = new Throwable();
                trace.setStackTrace(error.getInCodeSource());
                trace.printStackTrace();
            }
            Assertions.fail();
        }
        consumer.accept(result);
    }

    private static class InnerClass$InnerClass {
        public void test() {
            System.out.println("Hello, world!");
        }
    }

}
