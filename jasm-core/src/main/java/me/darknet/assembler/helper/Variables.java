package me.darknet.assembler.helper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NavigableMap;

/**
 * Container for containing method variables aka. locals
 *
 * @param parameters
 *                   Map of parameter index to name
 *                   <p>
 *                   They must be the names given to each parameter name of the
 *                   method defined in either the <a href=
 *                   "https://docs.oracle.com/javase/specs/jvms/se16/html/jvms-4.html#jvms-4.7.24">MethodParameters</a>
 *                   or be deduced via the <a href=
 *                   "https://docs.oracle.com/javase/specs/jvms/se16/html/jvms-4.html#jvms-4.7.13">LocalVariableTable</a>
 *                   if another variable in the LVT re-uses the same index as
 *                   the parameter use the first name given to the parameter in
 *                   this order:
 *                   <ol>
 *                   <li>MethodParameters</li>
 *                   <li>LocalVariableTable</li>
 *                   <ol>
 *                   <li>Use the first name that has the same type and
 *                   index</li>
 *                   </ol>
 *                   <li>Use a placeholder name p[n]</li>
 *                   </ol>
 * @param locals
 *                   A full list of all locals in the method
 */
public record Variables(@NotNull NavigableMap<Integer, Parameter> parameters, @NotNull List<Local> locals) {
    /**
     * Gets the variable <i>(local or parameter)</i> based on the index, code position, and assumed type.
     *
     * @param index
     *                 The index of the variable.
     * @param position
     *                 The position in code where it was used.
     * @param assumedTypeDesc
     *                 The assumed type descriptor of the variable to access.
     *                 This is implied from context and not assumed to be exact.
     *                 It is used to filter incompatible variables based on type categories.
     *
     * @return The local or parameter matching the index and position. Can be {@code null}.
     */
    public @Nullable Variable get(int index, int position, @NotNull String assumedTypeDesc) {
        int assumedTypeCategory = computeCategory(assumedTypeDesc.charAt(0));
        for (var local : locals)
            if (local.index == index
                    && local.start <= position && local.end >= position
                    && assumedTypeCategory == computeCategory(local.descriptor().charAt(0)))
                return local;
        return parameters.get(index);
    }

    private static int computeCategory(char c) {
        // Maps to ASM's Type.getSort()
        return switch (c) {
            case 'V' -> 0;
            case 'Z' -> 1;
            case 'C' -> 2;
            case 'B' -> 3;
            case 'S' -> 4;
            case 'I' -> 5;
            case 'F' -> 6;
            case 'J' -> 7;
            case 'D' -> 8;
            default -> -10; // Normally '[' is 9, but we want to treat arrays/objects as the same category group.
        };
    }

    public record Local(int index, int start, int end, String name, String descriptor) implements Variable {}

    public record Parameter(int index, String name, String descriptor) implements Variable {}

    public interface Variable {
        int index();
        @NotNull String name();
        @NotNull String descriptor();

        default boolean isPrimitive() {
            return descriptor().length() == 1;
        }
    }
}
