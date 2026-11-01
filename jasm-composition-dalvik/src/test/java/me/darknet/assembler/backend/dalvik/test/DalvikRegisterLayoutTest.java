package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.test.DiagnosticAssertions;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DalvikRegisterLayoutTest {
    @Test
    void infersNarrowAndWideLocalsBeforeWideParameters() {
        ClassDefinition definition = compile("""
                .super java/lang/Object
                .class public LayoutExample {
                    .method public static oneLocal (J)J {
                        parameters: { input },
                        code: {
                            const narrow 1
                            const-wide wide 2L
                            return-wide input
                        }
                    }
                    .method public static twoLocals (J)J {
                        parameters: { input },
                        code: {
                            const narrow 1
                            const another 3
                            const-wide wide 2L
                            return-wide input
                        }
                    }
                }
                """);

        Code oneLocal = definition.getMethod("oneLocal", "(J)J").getCode();
        assertEquals(5, oneLocal.getRegisters());
        assertEquals(2, oneLocal.getIn());
        assertEquals(3, returnInstruction(oneLocal).register());
        Code twoLocals = definition.getMethod("twoLocals", "(J)J").getCode();
        assertEquals(6, twoLocals.getRegisters());
        assertEquals(2, twoLocals.getIn());
        assertEquals(4, returnInstruction(twoLocals).register());

        String printed = print(definition);
        assertTrue(printed.contains("return-wide input"), printed);
    }

    @Test
    void laterWideUseGrowsTheLocalAreaWhenItsHighWordIsFree() {
        Code[] code = new Code[1];
        TestUtils.processDalvik("""
                .super java/lang/Object
                .class public LayoutExample {
                    .method public static widening ()V {
                        code: {
                            const value 1
                            invoke-static { value } demo/Target.takeInt (I)V
                            invoke-static { value } demo/Target.takeWide (J)V
                            return-void
                        }
                    }
                }
                """, TestUtils.options(), result -> code[0] = ((DalvikClassRepresentation) result.representation())
                .definition().getMethod("widening", "()V").getCode());

        assertEquals(2, code[0].getRegisters());
        assertEquals(0, code[0].getIn());
        assertEquals(List.of("0:value"), localBindings(code[0]));
    }

    @Test
    void keepsExplicitNumericLocalsBelowTailParameters() {
        ClassDefinition definition = compile("""
                .super java/lang/Object
                .class public LayoutExample {
                    .method public static high (I)I {
                        parameters: { input },
                        code: {
                            const v7 1
                            return input
                        }
                    }
                }
                """);
        Code code = definition.getMethod("high", "(I)I").getCode();

        assertEquals(9, code.getRegisters());
        assertEquals(1, code.getIn());
        assertEquals(8, returnInstruction(code).register());
        String printed = print(definition);
        assertTrue(printed.contains("const v7"), printed);
        assertTrue(printed.contains("return input"), printed);
    }

    @Test
    void printedCountFreeMethodsRecompileWithTheSameLocalAndParameterLayout() {
        ClassDefinition original = compile("""
                .super java/lang/Object
                .class public LayoutExample {
                    .method public static wide (J)J {
                        parameters: { input },
                        code: {
                            const narrow 1
                            const-wide wideValue 2L
                            return-wide input
                        }
                    }
                }
                """);
        Code originalCode = original.getMethod("wide", "(J)J").getCode();
        String printed = print(original);

        assertFalse(printed.contains("registers:"), printed);
        assertTrue(printed.contains("parameters: { input }"), printed);
        assertTrue(printed.contains("const narrow"), printed);
        assertTrue(printed.contains("const-wide wideValue"), printed);
        assertTrue(printed.contains("return-wide input"), printed);
        TestUtils.assertParsesDalvik(printed);

        Code recompiled = compile(printed).getMethod("wide", "(J)J").getCode();
        assertEquals(5, recompiled.getRegisters());
        assertEquals(originalCode.getRegisters(), recompiled.getRegisters());
        assertEquals(originalCode.getIn(), recompiled.getIn());
        assertEquals(List.of("input"), originalCode.getDebugInfo().parameterNames());
        assertEquals(originalCode.getDebugInfo().parameterNames(), recompiled.getDebugInfo().parameterNames());
        assertEquals(localBindings(originalCode), localBindings(recompiled));
        assertEquals(executableInstructions(originalCode), executableInstructions(recompiled));
        assertEquals(3, returnInstruction(recompiled).register());
    }

    @Test
    void leavesOccupiedWideHighWordConflictsToCodeEmission() {
        var outcome = TestUtils.compileDalvik("""
                .super java/lang/Object
                .class public LayoutExample {
                    .method public static conflict ()J {
                        code: {
                            const narrow 1
                            const v1 0
                            return-wide narrow
                        }
                    }
                }
                """, TestUtils.options());

        var conflict = outcome.errors().stream()
                .filter(diagnostic -> diagnostic.code() == DiagnosticCode.REGISTER_LIMIT)
                .findFirst()
                .orElseThrow(() -> new AssertionError(outcome.errors().toString()));
        assertTrue(conflict.message().contains("is already in use"), conflict.message());
        assertTrue(conflict.location() != null, "The wide-use conflict should point to its instruction");
        assertNull(outcome.requireValue().representation());
    }

    @Test
    void rejectsRegisterFilesLargerThanTheDexLimit() {
        var outcome = TestUtils.compileDalvik("""
                .super java/lang/Object
                .class public LayoutExample {
                    .method public static tooLarge ()V {
                        code: {
                            const v65535 1
                            return-void
                        }
                    }
                }
                """, TestUtils.options());

        DiagnosticAssertions.assertHasError(outcome, DiagnosticCode.TARGET_LIMIT,
                "Register files larger than 65535 words must be rejected");
        assertTrue(outcome.errors().stream().anyMatch(diagnostic -> diagnostic.message()
                .equals("Method requires more than 65535 Dalvik registers")));
        assertNull(outcome.requireValue().representation());
    }

    private static ReturnInstruction returnInstruction(Code code) {
        return code.getInstructions().stream()
                .filter(ReturnInstruction.class::isInstance)
                .map(ReturnInstruction.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static List<String> localBindings(Code code) {
        return code.getDebugInfo().locals().stream()
                .map(local -> local.register() + ":" + local.name())
                .toList();
    }

    private static List<Instruction> executableInstructions(Code code) {
        return code.getInstructions().stream()
                .filter(instruction -> !(instruction instanceof Label))
                .toList();
    }

    private static ClassDefinition compile(String source) {
        DalvikCompileResult result = DiagnosticAssertions.requireSuccess(
                TestUtils.compileDalvik(source, TestUtils.options()), "Failed to compile inferred Dalvik register layout");
        return ((DalvikClassRepresentation) result.representation()).definition();
    }

    private static String print(ClassDefinition definition) {
        PrintContext<?> context = new PrintContext<>("\t");
        new DalvikClassPrinter(definition).print(context);
        return TestUtils.normalize(context.toString());
    }
}
