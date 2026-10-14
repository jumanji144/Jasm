package me.darknet.assembler.instructions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable semantic metadata associated with one registered instruction.
 *
 * @param traits
 * 		Target-neutral traits carried by the instruction.
 * @param roles
 * 		Target-neutral roles assigned to instruction operands.
 * @param switchShape
 * 		Switch payload shape, or {@code null} when the instruction is not a switch.
 * @param canonicalName
 * 		Canonical source mnemonic used for aliases and lowering.
 * @param lowering
 * 		Target-owned lowering identity, or {@code null} for source-only forms such as
 * 		{@code line} that carry no target encoding.
 * @param unavailableReason
 * 		Reason this instruction cannot be lowered by the declaring target, or {@code null} when it is
 * 		available. Lets parsing separate "unknown instruction" from "known but unavailable here".
 */
public record InstructionMetadata(
        @NotNull EnumSet<InstructionTrait> traits,
        @NotNull List<OperandRole> roles,
        @Nullable SwitchShape switchShape,
        @NotNull String canonicalName,
        @Nullable InstructionLowering lowering,
        @Nullable String unavailableReason) {

    /**
     * Validates and freezes metadata supplied by an instruction registry.
     */
    public InstructionMetadata {
        Objects.requireNonNull(traits, "traits");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(canonicalName, "canonicalName");
        if (canonicalName.isBlank())
            throw new IllegalArgumentException("canonicalName must not be blank");

        EnumSet<InstructionTrait> copiedTraits = traits.isEmpty()
                ? EnumSet.noneOf(InstructionTrait.class)
                : EnumSet.copyOf(traits);
        List<OperandRole> copiedRoles = List.copyOf(roles);
        for (int i = 0; i < copiedRoles.size(); i++) {
            OperandRole role = Objects.requireNonNull(copiedRoles.get(i), "roles[" + i + "]");
            Objects.requireNonNull(role.kind(), "roles[" + i + "].kind");
            Objects.requireNonNull(role.width(), "roles[" + i + "].width");
            Objects.requireNonNull(role.referencePolicy(), "roles[" + i + "].referencePolicy");
            if (role.index() < 0)
                throw new IllegalArgumentException("Operand role index must be nonnegative: " + role.index());
            for (int j = 0; j < i; j++) {
                OperandRole previous = copiedRoles.get(j);
                if (previous.index() == role.index() && previous.kind() == role.kind())
                    throw new IllegalArgumentException("Duplicate operand role: " + role);
            }
        }

        if (copiedTraits.contains(InstructionTrait.SWITCH) && switchShape == null)
            throw new IllegalArgumentException("Switch instructions require a switch shape");
        if (copiedTraits.contains(InstructionTrait.TYPE_REFERENCE)
                && copiedRoles.stream().noneMatch(role -> role.kind() == OperandRole.RoleKind.TYPE))
            throw new IllegalArgumentException("Type-reference instructions require a TYPE operand role");
        if ((copiedTraits.contains(InstructionTrait.CONDITIONAL_BRANCH)
                || copiedTraits.contains(InstructionTrait.UNCONDITIONAL_BRANCH))
                && copiedRoles.stream().noneMatch(role -> role.kind() == OperandRole.RoleKind.LABEL))
            throw new IllegalArgumentException("Branch instructions require a LABEL operand role");
        if ((copiedTraits.contains(InstructionTrait.FIELD_REFERENCE)
                || copiedTraits.contains(InstructionTrait.METHOD_REFERENCE))
                && copiedRoles.stream().noneMatch(role -> role.kind() == OperandRole.RoleKind.MEMBER))
            throw new IllegalArgumentException("Member-reference instructions require a MEMBER operand role");
        if (copiedTraits.contains(InstructionTrait.FALLTHROUGH)
                && (copiedTraits.contains(InstructionTrait.UNCONDITIONAL_BRANCH)
                || copiedTraits.contains(InstructionTrait.RETURN)
                || copiedTraits.contains(InstructionTrait.THROW)
                || copiedTraits.contains(InstructionTrait.SWITCH)))
            throw new IllegalArgumentException("FALLTHROUGH cannot be combined with an instruction that leaves the flow");

        traits = copiedTraits;
        roles = copiedRoles;
    }

    /**
     * Creates metadata while keeping registry declarations compact.
     *
     * @return Validated immutable metadata.
     */
    public static @NotNull InstructionMetadata of(
            @NotNull EnumSet<InstructionTrait> traits,
            @NotNull List<OperandRole> roles,
            @Nullable SwitchShape switchShape,
            @NotNull String canonicalName,
            @Nullable InstructionLowering lowering,
            @Nullable String unavailableReason) {
        return new InstructionMetadata(traits, roles, switchShape, canonicalName, lowering, unavailableReason);
    }

    @Override
    public @NotNull EnumSet<InstructionTrait> traits() {
        return traits.clone();
    }

    @Override
    public @NotNull List<OperandRole> roles() {
        return roles;
    }
}
