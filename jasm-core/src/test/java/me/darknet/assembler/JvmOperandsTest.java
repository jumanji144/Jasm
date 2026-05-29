package me.darknet.assembler;

import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmOperandsTest {

    @Test
    void invokedynamicRequiresAnArgsArray() {
        assertErrorContains("""
                .method public static test ()V {
                  code: {
                    invokedynamic run ()V { invokestatic, example/Bootstrap.bootstrap, ()V } bad
                    return
                  }
                }
                """, "args");
    }

    @Test
    void lookupSwitchRequiresLabelTargets() {
        assertErrorContains("""
                .method public static test ()V {
                  code: {
                    lookupswitch { default: L0, 0: 1 }
                    return
                  }
                }
                """, "identifier");
    }

    @Test
    void tableSwitchRequiresIntegerBounds() {
        assertErrorContains("""
                .method public static test ()V {
                  code: {
                    tableswitch { min: 1.5, max: 2, default: L0, cases: { L1 } }
                    return
                  }
                }
                """, "integer literal");
    }

    private static void assertErrorContains(String source, String messagePart) {
        Result<?> result = AssemblyParseFixture.processAst("<test>", source, BytecodeFormat.JVM);
        DiagnosticAssertions.assertHasErrors(result, "Expected JVM operand validation errors");
        assertTrue(DiagnosticAssertions.formatErrors(result.errors()).contains(messagePart));
    }
}
