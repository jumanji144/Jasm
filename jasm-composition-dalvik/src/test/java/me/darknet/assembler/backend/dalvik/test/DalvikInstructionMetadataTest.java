package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.backend.dalvik.instructions.DalvikInstructions;
import me.darknet.assembler.backend.dalvik.instructions.DalvikOperands;
import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.instructions.OperandRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies completeness of Dalvik instruction metadata declarations.
 */
public class DalvikInstructionMetadataTest {
    @Test
    void filledNewArrayRangeUsesTypeOperandOne() {
        assertTrue(DalvikInstructions.INSTANCE.get("filled-new-array/range").typeReferenceOperandIndex() == 1);
    }

    @Test
    void memberReferenceOperandIsDeclared() {
        for (String name : List.of("iget", "iget-wide", "iput", "iput-object"))
            assertEquals(2, DalvikInstructions.INSTANCE.get(name).memberReferenceOperandIndex(), name);
        for (String name : List.of("sget", "sget-wide", "sput", "sput-object"))
            assertEquals(1, DalvikInstructions.INSTANCE.get(name).memberReferenceOperandIndex(), name);
        for (String name : List.of("invoke-virtual", "invoke-static/range", "invoke-interface"))
            assertEquals(1, DalvikInstructions.INSTANCE.get(name).memberReferenceOperandIndex(), name);
        for (String name : List.of("invoke-polymorphic", "invoke-polymorphic/range"))
            assertEquals(1, DalvikInstructions.INSTANCE.get(name).memberReferenceOperandIndex(), name);

        // invoke-custom dispatches through a bootstrap handle and filled-new-array allocates an array
        // type, so neither carries a member path.
        assertEquals(-1, DalvikInstructions.INSTANCE.get("invoke-custom").memberReferenceOperandIndex());
        assertEquals(-1, DalvikInstructions.INSTANCE.get("filled-new-array").memberReferenceOperandIndex());
    }

    @Test
    void declaredRoleWidthAgreesWithTheOperandSchema() {
        // The role and the operand schema state the same fact, so they can never disagree: a wide role
        // index is exactly the index whose schema resolves a wide register.
        for (String name : DalvikInstructions.INSTANCE.getInstructionNames()) {
            Instruction<?> instruction = DalvikInstructions.INSTANCE.get(name);
            assertNotNull(instruction, name);
            for (int index = 0; index < instruction.operandCount(); index++) {
                boolean schemaIsWide = instruction.operand(index) == DalvikOperands.REGISTER_WIDE.getOperand();
                OperandRole role = instruction.role(index);
                boolean roleIsWide = role != null && role.width() == OperandRole.WidthPolicy.WIDE;
                assertEquals(schemaIsWide, roleIsWide, name + " operand " + index);
            }
        }
    }

    /**
     * @param name
     * 		Registered mnemonic.
     *
     * @return Positions declaring a wide operand, in declaration order.
     */
    private static List<Integer> widePositions(String name) {
        Instruction<?> instruction = DalvikInstructions.INSTANCE.get(name);
        assertNotNull(instruction, name);
        return instruction.metadata().roles().stream()
                .filter(role -> role.width() == OperandRole.WidthPolicy.WIDE)
                .map(OperandRole::index)
                .toList();
    }

    @Test
    void wideOperandsAreDeclaredAtThePositionThatHoldsTheWideValue() {
        // Position matters, not presence. Checking only that some role was wide let both width bugs
        // through: a long shift declared its narrow distance wide, and the conversions plus the wide
        // array and field forms declared nothing wide at all. Every expectation below comes from the
        // dex encoding, so a suffix-derived rule cannot satisfy it by accident.
        assertEquals(List.of(0, 1), widePositions("move-wide"));
        assertEquals(List.of(0), widePositions("move-result-wide"));
        assertEquals(List.of(0), widePositions("return-wide"));
        assertEquals(List.of(0), widePositions("const-wide"));

        // Arithmetic and bitwise long/double operations are wide on all three operands.
        for (String name : List.of("add-long", "sub-long", "mul-long", "div-long", "rem-long", "and-long",
                "or-long", "xor-long", "add-double", "sub-double", "mul-double", "div-double", "rem-double"))
            assertEquals(List.of(0, 1, 2), widePositions(name), name);
        for (String name : List.of("add-long/2addr", "rem-long/2addr", "add-double/2addr", "rem-double/2addr"))
            assertEquals(List.of(0, 1), widePositions(name), name);

        // A long shift's distance is an int, so only the destination and value are wide.
        for (String name : List.of("shl-long", "shr-long", "ushr-long"))
            assertEquals(List.of(0, 1), widePositions(name), name);
        for (String name : List.of("shl-long/2addr", "shr-long/2addr", "ushr-long/2addr"))
            assertEquals(List.of(0), widePositions(name), name);

        // The compared values are wide but the result is an int.
        for (String name : List.of("cmp-long", "cmpl-double", "cmpg-double"))
            assertEquals(List.of(1, 2), widePositions(name), name);
        for (String name : List.of("cmpl-float", "cmpg-float"))
            assertEquals(List.of(), widePositions(name), name);

        // Negation and complement are wide on both operands for the long and double types.
        for (String name : List.of("neg-long", "not-long", "neg-double"))
            assertEquals(List.of(0, 1), widePositions(name), name);
        for (String name : List.of("neg-int", "not-int", "neg-float"))
            assertEquals(List.of(), widePositions(name), name);

        // A conversion is wide on whichever side names a long or a double, never on a fixed position.
        for (String name : List.of("int-to-long", "int-to-double", "float-to-long", "float-to-double"))
            assertEquals(List.of(0), widePositions(name), name);
        for (String name : List.of("long-to-int", "long-to-float", "double-to-int", "double-to-float"))
            assertEquals(List.of(1), widePositions(name), name);
        for (String name : List.of("long-to-double", "double-to-long"))
            assertEquals(List.of(0, 1), widePositions(name), name);
        for (String name : List.of("int-to-float", "float-to-int", "int-to-byte", "int-to-char", "int-to-short"))
            assertEquals(List.of(), widePositions(name), name);

        // The loaded or stored value is wide; the array, index and object are not.
        assertEquals(List.of(0), widePositions("aget-wide"));
        assertEquals(List.of(0), widePositions("aput-wide"));
        for (String name : List.of("aget", "aget-object", "aput", "aput-object"))
            assertEquals(List.of(), widePositions(name), name);

        // The field value is wide; a static operation reads one register and an instance operation two.
        for (String name : List.of("iget-wide", "iput-wide", "sget-wide", "sput-wide"))
            assertEquals(List.of(0), widePositions(name), name);
        for (String name : List.of("iget", "iget-object", "iput", "sget", "sput-object"))
            assertEquals(List.of(), widePositions(name), name);
    }

    @Test
    void narrowFormsDeclareNoWideOperand() {
        // The literal forms are integer-only, so neither their roles nor their schemas are wide.
        for (String name : List.of("add-int/lit8", "rsub-int", "shl-int/lit8", "add-int"))
            assertEquals(List.of(), widePositions(name), name);
        assertFalse(DalvikInstructions.INSTANCE.get("add-int").metadata().roles().stream()
                .anyMatch(role -> role.width() == OperandRole.WidthPolicy.WIDE));
    }

    @Test
    void controlFlowAndReferenceTraitsAreDeclared() {
        assertTraits("goto", InstructionTrait.UNCONDITIONAL_BRANCH);
        assertTraits("if-eq", InstructionTrait.CONDITIONAL_BRANCH, InstructionTrait.FALLTHROUGH);
        assertTraits("packed-switch", InstructionTrait.SWITCH, InstructionTrait.PAYLOAD);
        assertTraits("sparse-switch", InstructionTrait.SWITCH, InstructionTrait.PAYLOAD);
        assertTraits("throw", InstructionTrait.THROW);
        assertTraits("return", InstructionTrait.RETURN);
        assertTraits("return-void", InstructionTrait.RETURN);
        assertTraits("invoke-virtual", InstructionTrait.INVOKE, InstructionTrait.METHOD_REFERENCE,
                InstructionTrait.FALLTHROUGH);
        assertTraits("iget", InstructionTrait.FIELD_REFERENCE, InstructionTrait.FALLTHROUGH);
        assertTraits("new-instance", InstructionTrait.TYPE_REFERENCE, InstructionTrait.FALLTHROUGH);
        assertTraits("move", InstructionTrait.REGISTER_READ, InstructionTrait.REGISTER_WRITE);
        assertTraits("const", InstructionTrait.REGISTER_WRITE);
        assertTraits("add-int", InstructionTrait.REGISTER_READ, InstructionTrait.REGISTER_WRITE);
        assertTraits("fill-array-data", InstructionTrait.PAYLOAD, InstructionTrait.REGISTER_READ);
        assertTraits("line", InstructionTrait.PSEUDO, InstructionTrait.DEBUG_METADATA);
    }

    private static void assertTraits(String name, InstructionTrait... traits) {
        Instruction<?> instruction = DalvikInstructions.INSTANCE.get(name);
        assertNotNull(instruction, name);
        for (InstructionTrait trait : traits)
            assertTrue(instruction.hasTrait(trait), name + " should be " + trait);
        // A fall through is expected of everything that keeps executing, and of nothing that leaves the
        // flow; a pseudo-instruction carries no control-flow claim either way.
        if (instruction.hasTrait(InstructionTrait.PSEUDO))
            return;
        if (!List.of(traits).contains(InstructionTrait.UNCONDITIONAL_BRANCH)
                && !List.of(traits).contains(InstructionTrait.RETURN)
                && !List.of(traits).contains(InstructionTrait.THROW)
                && !List.of(traits).contains(InstructionTrait.SWITCH))
            assertTrue(instruction.hasTrait(InstructionTrait.FALLTHROUGH), name + " should fall through");
        else
            assertFalse(instruction.hasTrait(InstructionTrait.FALLTHROUGH), name + " leaves the flow");
    }

    @Test
    void everyInstructionHasRequiredMetadata() {
        for (String name : DalvikInstructions.INSTANCE.getInstructionNames()) {
            Instruction<?> instruction = DalvikInstructions.INSTANCE.get(name);
            assertNotNull(instruction, name);
            assertTrue(instruction.lowering() != null || "line".equals(name), name);
            // A branch names its target, and leaving the instruction and falling through are
            // mutually exclusive outcomes.
            if (instruction.hasTrait(InstructionTrait.CONDITIONAL_BRANCH)
                    || instruction.hasTrait(InstructionTrait.UNCONDITIONAL_BRANCH))
                assertTrue(instruction.metadata().roles().stream()
                        .anyMatch(role -> role.kind() == OperandRole.RoleKind.LABEL), name);
            if (instruction.hasTrait(InstructionTrait.FALLTHROUGH))
                assertFalse(instruction.hasTrait(InstructionTrait.UNCONDITIONAL_BRANCH)
                        || instruction.hasTrait(InstructionTrait.RETURN)
                        || instruction.hasTrait(InstructionTrait.THROW)
                        || instruction.hasTrait(InstructionTrait.SWITCH), name);
            if (instruction.hasTrait(InstructionTrait.TYPE_REFERENCE))
                assertTrue(instruction.typeReferenceOperandIndex() >= 0, name);
            for (OperandRole role : instruction.metadata().roles())
                assertEquals(referencePolicyFor(role.kind()), role.referencePolicy(), name);
            if (instruction.hasTrait(InstructionTrait.FIELD_REFERENCE)
                    || instruction.hasTrait(InstructionTrait.METHOD_REFERENCE))
                assertTrue(instruction.memberReferenceOperandIndex() >= 0, name);
            if (instruction.hasTrait(InstructionTrait.SWITCH))
                assertNotNull(instruction.switchShape(), name);
        }
    }

    /**
     * @param kind
     * 		Operand role kind.
     *
     * @return Reference policy the kind implies, so a role cannot state a contradictory pair.
     */
    private static OperandRole.ReferencePolicy referencePolicyFor(OperandRole.RoleKind kind) {
        return switch (kind) {
            case LABEL -> OperandRole.ReferencePolicy.LABEL;
            case TYPE -> OperandRole.ReferencePolicy.TYPE;
            case SWITCH_PAYLOAD -> OperandRole.ReferencePolicy.PAYLOAD;
            case MEMBER -> OperandRole.ReferencePolicy.MEMBER;
            case VARIABLE -> OperandRole.ReferencePolicy.NONE;
        };
    }
}
