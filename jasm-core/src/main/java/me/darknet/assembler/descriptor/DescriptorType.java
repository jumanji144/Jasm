package me.darknet.assembler.descriptor;

/**
 * A descriptor that names one value type rather than a method signature.
 */
public sealed interface DescriptorType extends Descriptor permits PrimitiveType, ClassDescriptor, ArrayDescriptor {
}
