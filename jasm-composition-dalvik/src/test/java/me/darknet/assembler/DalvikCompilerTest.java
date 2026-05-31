package me.darknet.assembler;

import me.darknet.assembler.compile.DalvikCompilerOptions;
import me.darknet.assembler.compiler.EmptyInheritanceChecker;
import me.darknet.assembler.printer.DalvikClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.type.Types;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikCompilerTest {

    @Test
    void compilesClassToDalvikRepresentation() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .field public static final answer I { value: 42 }
                    .method public static hello ()V {
                        code: {
                            return-void
                        }
                    }
                }
                """, options(), result -> {
            assertTrue(result.representation() instanceof DalvikClassRepresentation);

            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertEquals("Example", definition.getType().internalName());
            assertNotNull(definition.getSuperClass());
            assertEquals("java/lang/Object", definition.getSuperClass().internalName());
            assertNotNull(definition.getField("answer", "I"));
            assertNotNull(definition.getMethod("hello", "()V"));
            assertNotNull(definition.getMethod("hello", "()V").getCode());
            assertEquals(1, definition.getMethod("hello", "()V").getCode().getInstructions().size());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains(".super java/lang/Object"), printed);
            assertTrue(printed.contains(".field public static final answer I {value: 42}"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void compilesTopLevelFieldIntoOverlayClass() {
        TestUtils.processDalvik(
                ".field public static final answer I { value: 42 }",
                overlayOptions("top/level/OverlayExample"),
                result -> {
                    ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
                    assertEquals("top/level/OverlayExample", definition.getType().internalName());
                    assertNotNull(definition.getField("answer", "I"));
                }
        );
    }

    @Test
    void compilesTopLevelMethodIntoOverlayClass() {
        TestUtils.processDalvik("""
                .method public static hello ()V {
                    code: {
                        return-void
                    }
                }
                """, overlayOptions("top/level/OverlayExample"), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertEquals("top/level/OverlayExample", definition.getType().internalName());
            assertNotNull(definition.getMethod("hello", "()V"));
            assertNotNull(definition.getMethod("hello", "()V").getCode());
            assertEquals(1, definition.getMethod("hello", "()V").getCode().getInstructions().size());
        });
    }

    @Test
    void compilesBranchingAndTypeInstructions() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static branching (Ljava/lang/Object;[I)I {
                        parameters: { value, array },
                        code: {
                        A:
                            check-cast value Ljava/lang/String;
                            instance-of v0 value Ljava/lang/String;
                            if-eqz v0 B
                            array-length v1 array
                            goto C
                        B:
                            new-instance v2 Ljava/lang/RuntimeException;
                            throw v2
                        C:
                            return v1
                        }
                    }
                }
                """, options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            assertNotNull(definition.getMethod("branching", "(Ljava/lang/Object;[I)I"));
            assertNotNull(definition.getMethod("branching", "(Ljava/lang/Object;[I)I").getCode());
            assertEquals(11, definition.getMethod("branching", "(Ljava/lang/Object;[I)I").getCode().getInstructions().size());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains("if-eqz"), printed);
            assertTrue(printed.contains("new-instance"), printed);
            assertTrue(printed.contains("throw"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    @Test
    void compilesArrayPayloadsAndExceptionTables() {
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public Example {
                    .method public static arrays ([I)V {
                        parameters: { array },
                        exceptions: { { Start, End, Handler, java/lang/RuntimeException } },
                        code: {
                        Start:
                            fill-array-data array { 1, 2, 3 }
                            filled-new-array/range { v0, v2 } [I
                            move-result-object v3
                        End:
                            return-void
                        Handler:
                            move-exception v4
                            return-void
                        }
                    }
                }
                """, options(), result -> {
            ClassDefinition definition = ((DalvikClassRepresentation) result.representation()).definition();
            var method = definition.getMethod("arrays", "([I)V");
            assertNotNull(method);
            assertNotNull(method.getCode());
            assertEquals(1, method.getCode().tryCatch().size());
            assertEquals(1, method.getCode().tryCatch().getFirst().handlers().size());

            DalvikClassPrinter printer = new DalvikClassPrinter(definition);
            PrintContext<?> ctx = new PrintContext<>("\t");
            printer.print(ctx);

            String printed = TestUtils.normalize(ctx.toString());
            assertTrue(printed.contains("exceptions: { {"), printed);
            assertTrue(printed.contains("java/lang/RuntimeException"), printed);
            assertTrue(printed.contains("fill-array-data"), printed);
            assertTrue(printed.contains("{ 0x1, 0x2, 0x3 }"), printed);
            assertTrue(printed.contains("filled-new-array/range"), printed);
            assertTrue(printed.contains("[I"), printed);
            TestUtils.assertParsesDalvik(printed);
        });
    }

    private static DalvikCompilerOptions options() {
        return new DalvikCompilerOptions()
                .version(35)
                .inheritanceChecker(EmptyInheritanceChecker.INSTANCE);
    }

    private static DalvikCompilerOptions overlayOptions(String internalName) {
        ClassDefinition overlay = new ClassDefinition(
                Types.instanceTypeFromInternalName(internalName),
                Types.instanceTypeFromInternalName("java/lang/Object"),
                DalvikModifiers.ACC_PUBLIC
        );
        return options().overlay(new DalvikClassRepresentation(overlay));
    }
}
