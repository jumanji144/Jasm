package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.ValidatedOperand;
import me.darknet.assembler.test.FixtureTarget;
import me.darknet.assembler.processing.ProcessedInstruction;
import me.darknet.assembler.processing.ProcessedLabel;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.processing.PartialProcessedUnit;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import me.darknet.assembler.visitor.Modifiers;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests source-to-definition and typed-role associations in processed views.
 */
public class SemanticProcessorTest {
    @Test
    void preservesJvmDefinitionAndLabelAlignment() {
        ValidatedUnit unit = processJvm("""
                .method demo ()V {
                  code: {
                    Start:
                    ldc 1L
                    goto Start
                    return
                  }
                }
                """);
        ProcessedMethod method = unit.methods().values().iterator().next();
        assertInstanceOf(ProcessedLabel.class, method.code().getFirst());
        ProcessedInstruction ldc = assertInstanceOf(ProcessedInstruction.class, method.code().get(1));
        ProcessedInstruction jump = assertInstanceOf(ProcessedInstruction.class, method.code().get(2));
        assertSame(FixtureTarget.JVM.context().instructions().get("ldc"), ldc.definition());
        assertSame(FixtureTarget.JVM.context().instructions().get("goto"), jump.definition());
        assertEquals(0, jump.definition().metadata().roles().getFirst().index());

        // One entry per operand, index-aligned, so lookups by index cannot drift from the schema.
        assertIndexAligned(jump);
        assertEquals(0, jump.operands().getFirst().index());
        assertEquals(OperandRole.RoleKind.LABEL, jump.role(0).kind());
        assertSame(jump.source().arguments().getFirst(), jump.operands().getFirst().source());
    }

    @Test
    void recordsDalvikTypeRoleAtActualRangeOperand() {
        ValidatedUnit unit = processDalvik("""
                .method demo ()V {
                  code: {
                    filled-new-array/range { v0, v1 } [I
                    return-void
                  }
                }
                """);
        ProcessedInstruction instruction = assertInstanceOf(
                ProcessedInstruction.class,
                unit.methods().values().iterator().next().code().getFirst()
        );
        assertSame(FixtureTarget.DALVIK.context().instructions().get("filled-new-array/range"), instruction.definition());
        assertIndexAligned(instruction);

        // Index 0 is the register array and declares no role, so the type role lives at index 1.
        assertEquals(2, instruction.operands().size());
        assertNull(instruction.role(0));
        assertEquals(1, instruction.role(1).index());
        assertEquals(OperandRole.RoleKind.TYPE, instruction.role(1).kind());
        assertInstanceOf(ASTIdentifier.class, instruction.operands().get(1).source());
        assertSame(instruction.operands().get(1).source(), instruction.source().arguments().get(1));
    }

    @Test
    void recordsJvmSwitchPayloadAndFlowRoles() {
        ValidatedUnit unit = processJvm("""
                .method demo ()V {
                  code: {
                    tableswitch { min: 0, max: 1, default: Out, cases: { Out, Out } }
                    lookupswitch { 0: Out, default: Out }
                    goto Out
                  Out:
                    return
                  }
                }
                """);
        List<me.darknet.assembler.processing.ProcessedCodeEntry> code =
                unit.methods().values().iterator().next().code();

        ProcessedInstruction table = assertInstanceOf(ProcessedInstruction.class, code.get(0));
        assertEquals(me.darknet.assembler.instructions.SwitchShape.TABLE, table.definition().switchShape());
        assertEquals(OperandRole.RoleKind.SWITCH_PAYLOAD, table.role(0).kind());
        assertEquals(0, table.role(0).index());

        ProcessedInstruction lookup = assertInstanceOf(ProcessedInstruction.class, code.get(1));
        assertEquals(me.darknet.assembler.instructions.SwitchShape.LOOKUP, lookup.definition().switchShape());

        ProcessedInstruction jump = assertInstanceOf(ProcessedInstruction.class, code.get(2));
        assertEquals(OperandRole.RoleKind.LABEL, jump.role(0).kind());
        assertInstanceOf(ASTIdentifier.class, jump.operands().get(0).source());
    }

    @Test
    void recordsDalvikSwitchPayloadAndBranchLabelRoles() {
        ValidatedUnit unit = processDalvik("""
                .method demo ()V {
                  code: {
                    if-eq v0 v1 Out
                    packed-switch v0 { first: 0, targets: { Out } }
                    sparse-switch v1 { 4: Out }
                  Out:
                    return-void
                  }
                }
                """);
        List<me.darknet.assembler.processing.ProcessedCodeEntry> code =
                unit.methods().values().iterator().next().code();

        ProcessedInstruction ifEq = assertInstanceOf(ProcessedInstruction.class, code.get(0));
        assertEquals(OperandRole.RoleKind.LABEL, ifEq.role(2).kind());
        assertEquals(2, ifEq.role(2).index());

        ProcessedInstruction packed = assertInstanceOf(ProcessedInstruction.class, code.get(1));
        assertEquals(me.darknet.assembler.instructions.SwitchShape.PACKED, packed.definition().switchShape());
        assertEquals(1, packed.role(1).index());
        assertIndexAligned(packed);

        ProcessedInstruction sparse = assertInstanceOf(ProcessedInstruction.class, code.get(2));
        assertEquals(me.darknet.assembler.instructions.SwitchShape.SPARSE, sparse.definition().switchShape());
        assertEquals(1, sparse.role(1).index());
    }

    @Test
    void associatesCanonicalAliasesAndPreservesMemberOperands() {
        ValidatedUnit unit = processJvm("""
                .method demo ()V {
                  code: {
                    ldc2_w 1L
                    getstatic java/lang/System.out Ljava/io/PrintStream;
                    invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
                    return
                  }
                }
                """);
        List<me.darknet.assembler.processing.ProcessedCodeEntry> code =
                unit.methods().values().iterator().next().code();

        ProcessedInstruction ldc2 = assertInstanceOf(ProcessedInstruction.class, code.get(0));
        assertEquals("ldc", ldc2.definition().canonicalName());
        assertEquals("ldc2_w", ldc2.definition().name());

        // Fixture operands declare no resolver, so an operand without a typed schema keeps exposing
        // only its source node, and emitters must read it exactly as before.
        ProcessedInstruction getstatic = assertInstanceOf(ProcessedInstruction.class, code.get(1));
        ASTInstruction getstaticSource = getstatic.source();
        assertEquals(2, getstaticSource.arguments().size());
        assertEquals("java/lang/System.out", getstaticSource.arguments().get(0).content());
        assertEquals("Ljava/io/PrintStream;", getstaticSource.arguments().get(1).content());
        assertNull(getstatic.operand(0));
        assertSame(getstaticSource.arguments().get(0), getstatic.operands().get(0).source());

        ProcessedInstruction invoke = assertInstanceOf(ProcessedInstruction.class, code.get(2));
        assertEquals(2, invoke.source().arguments().size());
        assertEquals("java/io/PrintStream.println", invoke.source().arguments().get(0).content());
        assertEquals("(Ljava/lang/String;)V", invoke.source().arguments().get(1).content());
    }

    @Test
    void reportsUnknownInstructionDuringSemanticLowering() {
        TargetContext target = FixtureTarget.JVM.context();
        List<ASTElement> ast = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processDeclarations("<test>", """
                        .method demo ()V {
                          code: {
                            nope
                          }
                        }
                        """, target),
                "Unknown instruction names remain source syntax before semantic lowering"
        );

        var result = SemanticProcessor.process(ast, target);

        DiagnosticAssertions.assertHasErrors(result, "Unknown instruction should fail semantic processing");
        assertEquals(1, result.errors().size(), DiagnosticAssertions.formatErrors(result.errors()));
        DiagnosticAssertions.assertPhase(result.errors(), DiagnosticPhase.SEMANTIC_LOWERING,
                "Instruction lookup is part of semantic lowering");
        DiagnosticAssertions.assertHasErrorCode(result.errors(), DiagnosticCode.UNKNOWN_INSTRUCTION,
                "An unregistered mnemonic should be reported as an unknown instruction");
        assertTrue(DiagnosticAssertions.formatErrors(result.errors()).contains("Unknown instruction: nope"),
                DiagnosticAssertions.formatErrors(result.errors()));
    }

    @Test
    void rejectsInvalidProcessedInstructionBeforeEmission() {
        ASTInstruction invalid = new ASTInstruction(
                tokenIdentifier("goto", 20),
                java.util.Collections.singletonList(null)
        );
        ASTMethod method = new ASTMethod(
                new Modifiers(),
                tokenIdentifier("demo", 0),
                tokenIdentifier("()V", 5),
                List.of(),
                java.util.Map.of(),
                List.of(),
                null,
                List.of(),
                new ASTCode(List.of(invalid)),
                ElementMap.empty()
        );
        var result = SemanticProcessor.process(
                List.of(method), FixtureTarget.JVM.context()
        );
        DiagnosticAssertions.assertHasErrors(result, "Semantic processing must reject invalid operands");
        assertEquals(1, result.errors().size(), DiagnosticAssertions.formatErrors(result.errors()));
        DiagnosticAssertions.assertPhase(result.errors(), DiagnosticPhase.SEMANTIC_LOWERING,
                "Invalid operands are validated during semantic lowering");
    }

    @Test
    void reportsEveryMethodThatCannotBeLowered() {
        // A source with several bad methods must report all of them. Processing used to stop at the first
        // failure, which hid every later problem in the same file. The methods are built directly so the
        // failure under test can only come from semantic processing.
        ASTMethod first = method("first", List.of(invalidInstruction("goto", 10)));
        ASTMethod second = method("second", List.of(invalidInstruction("ldc", 40)));
        ASTMethod third = method("third", List.of());

        var result = SemanticProcessor.processPartial(List.of(first, second, third), FixtureTarget.JVM.context());

        DiagnosticAssertions.assertHasErrors(result, "Both bad methods should be reported");
        assertEquals(2, result.errors().size(), DiagnosticAssertions.formatErrors(result.errors()));
        DiagnosticAssertions.assertHasErrorCode(result.errors(), DiagnosticCode.OPERAND_SHAPE,
                "A missing operand should be reported as an operand shape problem");
        DiagnosticAssertions.assertPhase(result.errors(), DiagnosticPhase.SEMANTIC_LOWERING,
                "The operand schema runs while lowering the method");

        // The recovery view is still produced, and it holds exactly the method that lowered.
        PartialProcessedUnit unit = assertInstanceOf(PartialProcessedUnit.class, result.requireValue());
        assertEquals(1, unit.methods().size(), "Only the valid method should be lowered");
        assertEquals("third", unit.methods().values().iterator().next().source().getName().content());

        // The strict entry point must not expose that incomplete view to a compiler.
        var strict = SemanticProcessor.process(List.of(first, second, third), FixtureTarget.JVM.context());
        assertTrue(strict.isFailure(), "Semantic errors must produce a failed strict outcome");
        assertFalse(strict.hasValue(), "A failed strict outcome must not expose a unit");
    }

    /**
     * @param name
     * 		Method name.
     * @param instructions
     * 		Instructions the body declares.
     *
     * @return Method with an empty body carrying the given instructions.
     */
    private static ASTMethod method(String name, List<ASTInstruction> instructions) {
        return new ASTMethod(
                new Modifiers(),
                tokenIdentifier(name, 0),
                tokenIdentifier("()V", 0),
                List.of(),
                java.util.Map.of(),
                List.of(),
                null,
                List.of(),
                new ASTCode(instructions),
                ElementMap.empty()
        );
    }

    /**
     * @param mnemonic
     * 		Instruction the target registers.
     * @param start
     * 		Source offset used to keep the generated locations distinct.
     *
     * @return Instruction that declares a missing operand.
     */
    private static ASTInstruction invalidInstruction(String mnemonic, int start) {
        return new ASTInstruction(tokenIdentifier(mnemonic, start), java.util.Collections.singletonList(null));
    }

    private static void assertIndexAligned(ProcessedInstruction instruction) {
        List<ValidatedOperand> operands = instruction.operands();
        assertEquals(instruction.definition().operandCount(), operands.size(),
                "Every declared operand must carry one validated entry");
        for (int i = 0; i < operands.size(); i++) {
            assertEquals(i, operands.get(i).index(), "Operand entry must be index-aligned");
            assertSame(operands.get(i).value(), instruction.operand(i), "operand(int) must resolve by index");
            assertEquals(instruction.definition().role(i), instruction.role(i), "role(int) must resolve from the definition");
        }
    }

    private static ASTIdentifier tokenIdentifier(String content, int start) {
        return new ASTIdentifier(new Token(
                new Range(start, start + content.length() - 1),
                new Location(1, start + 1, content.length(), null),
                TokenType.IDENTIFIER,
                content
        ));
    }

    private static ValidatedUnit processJvm(String source) {
        return process(source, FixtureTarget.JVM.context());
    }

    private static ValidatedUnit processDalvik(String source) {
        return process(source, FixtureTarget.DALVIK.context());
    }

    private static ValidatedUnit process(String source, TargetContext target) {
        List<ASTElement> ast = DiagnosticAssertions.requireSuccess(
                AssemblyParseFixture.processDeclarations("<test>", source, target),
                "Source should parse"
        );
        return DiagnosticAssertions.requireSuccess(SemanticProcessor.process(ast, target), "Source should process semantically");
    }
}
