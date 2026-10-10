package me.darknet.assembler.backend.jvm.compile.analysis.jvm;

import me.darknet.assembler.analysis.Value;
import me.darknet.assembler.analysis.ValueMergeException;
import me.darknet.assembler.analysis.Values;
import me.darknet.assembler.backend.jvm.util.JvmTypeUtils;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.descriptor.ArrayDescriptor;
import me.darknet.assembler.descriptor.ClassDescriptor;
import me.darknet.assembler.descriptor.DescriptorType;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * Utility for merging two {@link Value} instances into a single value.
 */
public final class JvmValueMerger {
    private JvmValueMerger() { }

    /**
     * Merges two values into a single value.
     *
     * @param checker
     * 		Checker for determining common superclasses.
     * @param left
     * 		Left value to merge.
     * @param right
     * 		Right value to merge.
     *
     * @return Merged value.
     *
     * @throws ValueMergeException
     * 		When the two values cannot be merged.
     */
    public static @NotNull Value merge(@NotNull InheritanceChecker checker, @NotNull Value left,
                                       @NotNull Value right) throws ValueMergeException {
        if (left.equals(right))
            return left;
        if (left instanceof Value.TopValue || right instanceof Value.TopValue)
            return Values.TOP_VALUE;
        if (left instanceof Value.UninitializedReferenceValue)
            return Values.TOP_VALUE;
        if (left instanceof Value.PrimitiveValue leftPrimitive) {
            if (right instanceof Value.PrimitiveValue rightPrimitive
                    && leftPrimitive.type() == rightPrimitive.type())
                return Values.valueOfPrimitive(leftPrimitive.type());
            throw new ValueMergeException("Cannot merge primitive with non-primitive");
        }
        if (left instanceof Value.NullValue) {
            if (right instanceof Value.ObjectValue)
                return right;
            throw new ValueMergeException("Invalid merge of 'null' and non-object value");
        }
        if (left instanceof Value.ArrayValue leftArray)
            return mergeArray(checker, leftArray, right);
        if (left instanceof Value.ObjectValue leftObject)
            return mergeObject(checker, leftObject, right);
        throw new ValueMergeException("Cannot merge backend marker values");
    }

    /**
     * Merges an array value with another value.
     *
     * @param checker
     * 		Checker for determining common superclasses.
     * @param left
     * 		Left array value to merge.
     * @param right
     * 		Right value to merge.
     *
     * @return Merged value.
     *
     * @throws ValueMergeException
     * 		Thrown when the two values cannot be merged.
     */
    private static @NotNull Value mergeArray(@NotNull InheritanceChecker checker, @NotNull Value.ArrayValue left,
                                             @NotNull Value right) throws ValueMergeException {
        if (right instanceof Value.UninitializedReferenceValue)
            return Values.TOP_VALUE;
        if (right instanceof Value.ArrayValue rightArray) {
            Type common = JvmTypeUtils.commonType(checker,
                    JvmTypeUtils.toAsmType(left.arrayType()),
                    JvmTypeUtils.toAsmType(rightArray.arrayType()));
            if (common != null && common.getSort() == Type.ARRAY)
                return Values.valueOfArray((ArrayDescriptor) JvmTypeUtils.toDescriptorType(common));
            return Values.OBJECT_VALUE;
        }
        if (right instanceof Value.ObjectValue)
            return Values.OBJECT_VALUE;
        throw new ValueMergeException("Invalid array merge with non-object value");
    }

    /**
     * Merges an object value with another value.
     *
     * @param checker
     * 		Checker for determining common superclasses.
     * @param left
     * 		Left object value to merge.
     * @param right
     * 		Right value to merge.
     *
     * @return Merged value.
     *
     * @throws ValueMergeException
     * 		Thrown when the two values cannot be merged.
     */
    private static @NotNull Value mergeObject(@NotNull InheritanceChecker checker, @NotNull Value.ObjectValue left,
                                              @NotNull Value right) throws ValueMergeException {
        if (right instanceof Value.NullValue)
            return left;
        if (right instanceof Value.UninitializedReferenceValue)
            return Values.TOP_VALUE;
        if (right instanceof Value.ObjectValue rightObject) {
            DescriptorType leftType = left.type();
            DescriptorType rightType = rightObject.type();
            if (leftType == null || rightType == null)
                throw new ValueMergeException("Invalid merge of object and non-object value");
            String commonSuperclass = checker.getCommonSuperclass(
                    JvmTypeUtils.internalName(JvmTypeUtils.toAsmType(leftType)),
                    JvmTypeUtils.internalName(JvmTypeUtils.toAsmType(rightType)));
            ClassDescriptor common = new ClassDescriptor(commonSuperclass == null ? "java/lang/Object" : commonSuperclass);
            return Values.valueOfInstance(common);
        }
        throw new ValueMergeException("Invalid merge of object and non-object value");
    }
}
