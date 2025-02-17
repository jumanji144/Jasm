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
     * Gets either a local or a parameter based on the index and position.
     * If no local or parameter exists for then
     *
     * @param index
     *                 the index of the local var
     * @param position
     *                 the position in code where it was used
     *
     * @return the local or parameter matching the index and position.
     */
    public @Nullable Variable get(int index, int position) {
        for (var local : locals)
            if (local.index == index && local.start <= position && local.end >= position)
                return local;
	    return parameters.get(index);
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

        default int getPrimitiveKind() {
            // Maps to ASM's Type.getSort()
            return switch (descriptor().charAt(0)) {
                case 'V' -> 0;
                case 'Z' -> 1;
                case 'C' -> 2;
                case 'B' -> 3;
                case 'S' -> 4;
                case 'I' -> 5;
                case 'F' -> 6;
                case 'J' -> 7;
                case 'D' -> 8;
                default -> -1;
            };
        }
    }
}
