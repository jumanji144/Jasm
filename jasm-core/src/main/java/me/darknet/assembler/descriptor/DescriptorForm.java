package me.darknet.assembler.descriptor;

/**
 * Descriptor syntax accepted at an instruction operand position.
 */
public enum DescriptorForm {

    /**
     * A single-value type in descriptor form, void excluded: {@code I}, {@code Ljava/lang/String;},
     * {@code [I}. Used for field descriptors.
     */
    FIELD,

    /**
     * A method signature: {@code ()V}, {@code (I)Ljava/lang/String;}.
     */
    METHOD,

    /**
     * A reference type in descriptor form, so a class or array but never a primitive. Used where the
     * target needs an object reference, such as a Dalvik cast or allocation.
     */
    CLASS_OR_ARRAY,

    /**
     * Either a {@link #FIELD} or a {@link #METHOD}, decided by a leading {@code '('}. Used where one
     * operand position accepts both, such as a handle descriptor.
     */
    ANY,

    /**
     * A single-value type written as a descriptor or as an internal name: {@code java/lang/String}
     * and {@code Ljava/lang/String;} both mean the same type, and arrays are permitted. Used for JVM
     * type operands, which conventionally carry internal names.
     */
    TYPE_REFERENCE,

    /**
     * A class type written as a descriptor or as an internal name, never an array. Used where the
     * target allocates an instance and an array type would be meaningless.
     */
    CLASS_TYPE,

    /**
     * An internal name and nothing else: {@code java/lang/String}. Used where the target writes the
     * name straight into a class reference, which is a position that cannot hold a descriptor. A
     * descriptor here does not fail loudly, it produces a class named {@code Ljava/lang/String;},
     * so accepting it silently would be worse than rejecting it.
     */
    INTERNAL_NAME
}
