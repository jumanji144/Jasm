package me.darknet.assembler.backend.dalvik.analysis;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompilerOptions;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikRegisterUsage;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikRegisterUsageLookup;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class DalvikRegisterUsageLookupTest {
    @Test
    void countsAccessesAndKeepsReadWriteInstructionAsOneNavigationTarget() {
        Fixture fixture = compileMethod("""
                const v0 7
                move v1 v0
                add-int/2addr v1 v0
                return v1
                """, "()I", 2);
        List<DalvikRegisterUsage> usages = fixture.usages();

        DalvikRegisterUsage v0 = usage(usages, "v0");
        assertEquals(2, v0.readCount());
        assertEquals(1, v0.writeCount());
        assertEquals(List.of("const", "move", "add-int/2addr"), mnemonics(v0.references()));

        DalvikRegisterUsage v1 = usage(usages, "v1");
        assertEquals(2, v1.readCount());
        assertEquals(2, v1.writeCount());
        assertEquals(List.of("move", "add-int/2addr", "return"), mnemonics(v1.references()));
        assertEquals(1, v1.references().stream().filter(instruction -> mnemonic(instruction).equals("add-int/2addr")).count());
    }

    @Test
    void resolvesEquivalentMethodFromAnotherParseBySignature() {
        String code = """
                const v0 7
                move v1 v0
                return v1
                """;
        Fixture compiled = compileMethod(code, "()I", 2);
        Fixture reparsed = compileMethod(code, "()I", 2);
        assertNotSame(compiled.method(), reparsed.method());

        DalvikRegisterUsage v0 = usage(compiled.lookup().getUsages(reparsed.method()), "v0");
        assertEquals(1, v0.readCount());
        assertEquals(1, v0.writeCount());
        assertEquals(List.of("const", "move"), mnemonics(v0.references()));
    }

    @Test
    void countsDuplicateBranchOperandsButKeepsOneInstructionReference() {
        Fixture fixture = compileMethod("""
                const v0 0
                if-eq v0 v0 done
                return-void
            done:
                return-void
                """, "()V", 1);
        DalvikRegisterUsage v0 = usage(fixture.usages(), "v0");

        assertEquals(2, v0.readCount());
        assertEquals(1, v0.writeCount());
        assertEquals(List.of("const", "if-eq"), mnemonics(v0.references()));
        assertEquals(1, v0.references().stream().filter(instruction -> mnemonic(instruction).equals("if-eq")).count());
    }

    @Test
    void countsInvokeListsAndRangesAsTheirSourceOperands() {
        Fixture fixture = compileMethod("""
                invoke-static {v0, v1} LOwner;.call (II)V
                invoke-static/range {v1, v2} LOwner;.call (II)V
                return-void
                """, "()V", 3);
        List<DalvikRegisterUsage> usages = fixture.usages();

        assertEquals(1, usage(usages, "v0").readCount());
        assertEquals(1, usage(usages, "v1").readCount());
        assertEquals(List.of("invoke-static"), mnemonics(usage(usages, "v1").references()));

        DalvikRegisterUsage range = usage(usages, "{v1, v2}");
        assertEquals(1, range.readCount());
        assertEquals(0, range.writeCount());
        assertEquals(List.of("invoke-static/range"), mnemonics(range.references()));
    }

    @Test
    void classifiesEveryArrayAndFieldOpcodeVariantByAccessDirection() {
        List<String> code = new ArrayList<>();
        for (String variant : List.of("", "-wide", "-object", "-boolean", "-byte", "-char", "-short"))
            code.add("aget" + variant + " v1 v2 v3");
        for (String variant : List.of("", "-wide", "-object", "-boolean", "-byte", "-char", "-short"))
            code.add("aput" + variant + " v1 v2 v3");
        for (String variant : List.of("", "-wide", "-object", "-boolean", "-byte", "-char", "-short")) {
            String descriptor = fieldDescriptor(variant);
            code.add("iget" + variant + " v4 v5 LOwner;.instance " + descriptor);
            code.add("iput" + variant + " v4 v5 LOwner;.instance " + descriptor);
            code.add("sget" + variant + " v6 LOwner;.static " + descriptor);
            code.add("sput" + variant + " v6 LOwner;.static " + descriptor);
        }
        code.add("return-void");
        Fixture fixture = compileMethod(String.join("\n", code), "()V", 8);
        List<DalvikRegisterUsage> usages = fixture.usages();

        assertEquals(14, usage(usages, "v1").readCount());
        assertEquals(14, usage(usages, "v2").readCount());
        assertEquals(7, usage(usages, "v3").readCount());
        assertEquals(7, usage(usages, "v3").writeCount());
        assertEquals(7, usage(usages, "v4").readCount());
        assertEquals(7, usage(usages, "v4").writeCount());
        assertEquals(14, usage(usages, "v5").readCount());
        assertEquals(7, usage(usages, "v6").readCount());
        assertEquals(7, usage(usages, "v6").writeCount());
    }

    private static String fieldDescriptor(String variant) {
        return switch (variant) {
            case "-wide" -> "J";
            case "-object" -> "Ljava/lang/Object;";
            case "-boolean" -> "Z";
            case "-byte" -> "B";
            case "-char" -> "C";
            case "-short" -> "S";
            default -> "I";
        };
    }

    private static Fixture compileMethod(String code, String descriptor, int registers) {
        String source = """
                .super java/lang/Object
                .class public RegisterUsageFixture {
                    .method public static sample %s {
                        registers: %d,
                        code: {
                %s
                        }
                    }
                }
                """.formatted(descriptor, registers, indent(code));
        List<ASTElement> ast = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processDeclarations("<dalvik-register-usage-test>", source, DalvikTargetContext.INSTANCE),
                "Dalvik register usage source should parse");
        ValidatedUnit unit = DiagnosticAssertions.requireSuccess(
                SemanticProcessor.process(ast, DalvikTargetContext.INSTANCE),
                "Dalvik register usage source should process");
        assertEquals(1, unit.methods().size());
        ASTMethod method = unit.methods().keySet().iterator().next();
        assertSame(method, unit.methods().get(method).source());
        Outcome<DalvikCompileResult> compilation =
                new DalvikCompiler().compile(unit, new DalvikCompilerOptions().withVersion(35));
        assertFalse(compilation.errors().stream().anyMatch(diagnostic ->
                        diagnostic.phase() != DiagnosticPhase.OUTPUT_VERIFICATION ||
                                diagnostic.code() != DiagnosticCode.ANALYSIS_FAILURE),
                "Unexpected compiler errors: " + compilation.errors());
        DalvikCompileResult result = compilation.requireValue();
        DalvikRegisterUsageLookup lookup = result.registerUsageLookup();
        return new Fixture(method, lookup, lookup.getUsages(method));
    }

    private static String indent(String source) {
        return source.lines().map(line -> "            " + line).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static DalvikRegisterUsage usage(List<DalvikRegisterUsage> usages, String register) {
        return usages.stream()
                .filter(usage -> usage.registerName().equals(register))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing register usage " + register + ": " + usages));
    }

    private static List<String> mnemonics(List<ASTInstruction> instructions) {
        return instructions.stream().map(DalvikRegisterUsageLookupTest::mnemonic).toList();
    }

    private static String mnemonic(ASTInstruction instruction) {
        return instruction.identifier().literal();
    }

    private record Fixture(ASTMethod method, DalvikRegisterUsageLookup lookup, List<DalvikRegisterUsage> usages) {}
}
