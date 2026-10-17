package me.darknet.assembler.printer;

import java.util.Objects;

/**
 * Represents a site in the disassembly where a comment can be attached.
 */
public sealed interface DisassemblyCommentSite permits
        DisassemblyCommentSite.ClassSite,
        DisassemblyCommentSite.FieldSite,
        DisassemblyCommentSite.MethodSite,
        DisassemblyCommentSite.InstructionSite,
        DisassemblyCommentSite.LabelSite,
        DisassemblyCommentSite.MethodEndSite,
        DisassemblyCommentSite.ClassEndSite {

    /**
     * Represents a class site in the disassembly where a comment can be attached.
     *
     * @param classInternalName
     *        The internal name of the class.
     */
    record ClassSite(String classInternalName) implements DisassemblyCommentSite {
        public ClassSite {
            Objects.requireNonNull(classInternalName, "classInternalName");
        }
    }

    /**
     * Represents a field site in the disassembly where a comment can be attached.
     *
     * @param classInternalName
     *        The internal name of the class containing the field.
     * @param fieldName
     *        The name of the field.
     * @param descriptor
     *        The descriptor of the field.
     */
    record FieldSite(String classInternalName, String fieldName, String descriptor) implements DisassemblyCommentSite {
        public FieldSite {
            Objects.requireNonNull(classInternalName, "classInternalName");
            Objects.requireNonNull(fieldName, "fieldName");
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }

    /**
     * Represents a method site in the disassembly where a comment can be attached.
     *
     * @param classInternalName
     *        The internal name of the class containing the method.
     * @param methodName
     *        The name of the method.
     * @param descriptor
     *        The descriptor of the method.
     */
    record MethodSite(String classInternalName, String methodName, String descriptor) implements DisassemblyCommentSite {
        public MethodSite {
            Objects.requireNonNull(classInternalName, "classInternalName");
            Objects.requireNonNull(methodName, "methodName");
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }

    /**
     * Represents an instruction site in the disassembly where a comment can be attached.
     *
     * @param method
     *        The method site containing the instruction.
     * @param ordinal
     *        The ordinal of the instruction within the method.
     */
    record InstructionSite(MethodSite method, int ordinal) implements DisassemblyCommentSite {
        public InstructionSite {
            Objects.requireNonNull(method, "method");
            if (ordinal < 0)
                throw new IllegalArgumentException("ordinal must be non-negative");
        }
    }

    /**
     * Represents a label site in the disassembly where a comment can be attached.
     *
     * @param method
     *        The method site containing the label.
     * @param instructionOrdinal
     *        The ordinal of the instruction that the label is associated with.
     */
    record LabelSite(MethodSite method, int instructionOrdinal) implements DisassemblyCommentSite {
        public LabelSite {
            Objects.requireNonNull(method, "method");
            if (instructionOrdinal < 0)
                throw new IllegalArgumentException("instructionOrdinal must be non-negative");
        }
    }

    /**
     * Represents a method end site in the disassembly where a comment can be attached.
     *
     * @param method
     *        The method site that is ending.
     */
    record MethodEndSite(MethodSite method) implements DisassemblyCommentSite {
        public MethodEndSite {
            Objects.requireNonNull(method, "method");
        }
    }

    /**
     * Represents a class end site in the disassembly where a comment can be attached.
     *
     * @param classInternalName
     *        The internal name of the class that is ending.
     */
    record ClassEndSite(String classInternalName) implements DisassemblyCommentSite {
        public ClassEndSite {
            Objects.requireNonNull(classInternalName, "classInternalName");
        }
    }
}
