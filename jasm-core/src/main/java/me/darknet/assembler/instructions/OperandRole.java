package me.darknet.assembler.instructions;

import org.jetbrains.annotations.NotNull;

/**
 * Semantic role assigned to one instruction operand.
 *
 * @param index
 * 		Zero-based operand index.
 * @param kind
 * 		Semantic role of the operand.
 * @param width
 * 		Width of the value the operand holds.
 * @param referencePolicy
 * 		Whether the operand is a reference, and what kind. Declared separately from {@code kind}
 * 		because a reference can be spelled in more than one role: a member path is a reference without
 * 		being a label, a type or a payload.
 */
public record OperandRole(int index, @NotNull RoleKind kind, @NotNull WidthPolicy width,
                          @NotNull ReferencePolicy referencePolicy) {
    /**
     * Semantic categories currently needed by target-neutral queries and processing.
     */
    public enum RoleKind {
        LABEL,
        TYPE,
        SWITCH_PAYLOAD,
        VARIABLE,
        MEMBER
    }

    /**
     * How many storage words the operand's value occupies.
     */
    public enum WidthPolicy {
        SINGLE,
        WIDE
    }

    /**
     * Whether the operand resolves to an address or declaration elsewhere in the unit.
     */
    public enum ReferencePolicy {
        NONE,
        LABEL,
        TYPE,
        MEMBER,
        PAYLOAD
    }
}
