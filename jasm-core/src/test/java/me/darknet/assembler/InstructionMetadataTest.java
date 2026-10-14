package me.darknet.assembler;

import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.InstructionMetadata;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.SwitchShape;
import me.darknet.assembler.instructions.ValidatedOperand;
import me.darknet.assembler.test.FixtureLowering;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests target-neutral instruction metadata independently from backend-specific contracts.
 */
class InstructionMetadataTest {
    @Test
    void metadataCopiesMutableInputsAndReturnsDefensiveTraits() {
        EnumSet<InstructionTrait> traits = EnumSet.of(InstructionTrait.FIELD_REFERENCE);
        ArrayList<OperandRole> roles = new ArrayList<>(List.of(memberRole(0)));
        InstructionMetadata metadata = new InstructionMetadata(traits, roles, null, "getfield",
                new FixtureLowering("getfield"), null);

        traits.clear();
        roles.clear();
        assertEquals(EnumSet.of(InstructionTrait.FIELD_REFERENCE), metadata.traits());
        assertEquals(List.of(memberRole(0)), metadata.roles());

        EnumSet<InstructionTrait> returnedTraits = metadata.traits();
        returnedTraits.clear();
        assertTrue(metadata.traits().contains(InstructionTrait.FIELD_REFERENCE));
        assertThrows(UnsupportedOperationException.class, () -> metadata.roles().clear());
    }

    @Test
    void validatedOperandsRequireNonNegativeIndices() {
        ValidatedOperand operand = new ValidatedOperand(0, null,
                me.darknet.assembler.ast.primitive.ASTIdentifier.STUB);
        assertEquals(0, operand.index());
        assertThrows(IllegalArgumentException.class, () -> new ValidatedOperand(-1, null,
                me.darknet.assembler.ast.primitive.ASTIdentifier.STUB));
    }

    @Test
    void metadataRejectsInconsistentTraitsAndOperandRoles() {
        assertThrows(IllegalArgumentException.class, () -> metadata(EnumSet.of(InstructionTrait.SWITCH), List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> metadata(EnumSet.of(InstructionTrait.TYPE_REFERENCE), List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> metadata(EnumSet.of(InstructionTrait.CONDITIONAL_BRANCH), List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> metadata(EnumSet.of(InstructionTrait.FIELD_REFERENCE), List.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> metadata(EnumSet.of(InstructionTrait.FALLTHROUGH, InstructionTrait.RETURN), List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new InstructionMetadata(
                EnumSet.noneOf(InstructionTrait.class), List.of(new OperandRole(-1, OperandRole.RoleKind.LABEL,
                OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, "label", null, null));
        assertThrows(IllegalArgumentException.class, () -> new InstructionMetadata(
                EnumSet.noneOf(InstructionTrait.class), List.of(
                new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE,
                        OperandRole.ReferencePolicy.LABEL),
                new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE,
                        OperandRole.ReferencePolicy.LABEL)), null, "label", null, null));
        assertThrows(IllegalArgumentException.class, () -> new InstructionMetadata(
                EnumSet.noneOf(InstructionTrait.class), List.of(), null, " ", null, null));
    }

    @Test
    void registrySeparatesSourceKeywordsFromCanonicalAliasesAndKeepsOperandPositions() {
        MetadataInstructions registry = new MetadataInstructions();

        assertEquals(Set.of("getfield", "getfield_w", "multianewarray"), registry.getSourceKeywords());
        assertEquals(Set.of("getfield", "multianewarray"), registry.getCanonicalKeywords());
        assertEquals(List.of("getfield_w"), registry.aliasesOf("getfield"));

        Instruction<?> alias = registry.get("getfield_w");
        assertNotNull(alias);
        assertEquals("getfield_w", alias.name());
        assertEquals("getfield", alias.canonicalName());
        assertTrue(alias.hasTrait(InstructionTrait.FIELD_REFERENCE));
        assertTrue(alias.isAvailable());
        assertSame(registry.lowering, alias.lowering());

        Instruction<?> multiArray = registry.get("multianewarray");
        assertNotNull(multiArray);
        assertEquals(1, multiArray.typeReferenceOperandIndex());
        assertNull(multiArray.role(0));
        assertEquals(OperandRole.RoleKind.TYPE, multiArray.role(1).kind());
        assertFalse(multiArray.hasTrait(InstructionTrait.METHOD_REFERENCE));
    }

    private static InstructionMetadata metadata(EnumSet<InstructionTrait> traits, List<OperandRole> roles,
                                                SwitchShape shape) {
        return new InstructionMetadata(traits, roles, shape, "test", null, null);
    }

    private static OperandRole memberRole(int index) {
        return new OperandRole(index, OperandRole.RoleKind.MEMBER, OperandRole.WidthPolicy.SINGLE,
                OperandRole.ReferencePolicy.MEMBER);
    }

    private static final class MetadataInstructions extends Instructions<ASTInstructionVisitor> {
        private final FixtureLowering lowering = new FixtureLowering("getfield");

        private MetadataInstructions() {
            super(false);
            Operand[] twoOperands = { new Operand((context, element) -> {}), new Operand((context, element) -> {}) };
            EnumSet<InstructionTrait> memberTraits = EnumSet.of(InstructionTrait.FIELD_REFERENCE,
                    InstructionTrait.FALLTHROUGH);
            List<OperandRole> memberRoles = List.of(memberRole(0));
            register("getfield", twoOperands, (instruction, visitor) -> {},
                    metadata(memberTraits, memberRoles, null, "getfield", lowering));
            register("getfield_w", twoOperands, (instruction, visitor) -> {},
                    metadata(memberTraits, memberRoles, null, "getfield", lowering));
            register("multianewarray", twoOperands, (instruction, visitor) -> {},
                    metadata(EnumSet.of(InstructionTrait.TYPE_REFERENCE), List.of(new OperandRole(1,
                            OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE,
                            OperandRole.ReferencePolicy.TYPE)), null, "multianewarray", lowering));
            freeze();
        }

        @Override
        protected void registerInstructions() {
            // Definitions are installed after subclass initialization.
        }

        private InstructionMetadata metadata(EnumSet<InstructionTrait> traits, List<OperandRole> roles,
                                             SwitchShape shape, String canonicalName, FixtureLowering lowering) {
            return InstructionMetadata.of(traits, roles, shape, canonicalName, lowering, null);
        }
    }
}
