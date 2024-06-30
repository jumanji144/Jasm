package me.darknet.assembler.helper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NavigableMap;
import java.util.function.IntFunction;

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
public record Variables(@NotNull NavigableMap<Integer, String> parameters, @NotNull List<Local> locals) {

    /**
     * Gets a local var name based on the index and position.
     *
     * @param index
     *                 the index of the local var
     *
     * @return the name of the local, or {@code null} if no parameter var matches the index.
     */
    public @Nullable String getParameterName(int index) {
        if (parameters.isEmpty() || index > parameters.lastKey())
            return null;
        return parameters.get(index);
    }

    /**
     * Gets a local based on the index and position.
     *
     * @param index
     *                 the index of the local var
     * @param position
     *                 the position in code where it was used
     *
     * @return the local, or {@code null} if no var matches the index/position.
     */
    public @Nullable Local getLocal(int index, int position) {
        for (var local : locals)
            if (local.index == index && local.start <= position && local.end >= position)
                return local;
        return null;
    }

    /**
     * Gets a local var name based on the index and position.
     *
     * @param index
     *                 the index of the local var
     * @param position
     *                 the position in code where it was used
     *
     * @return the name of the local, or {@code null} if no var matches the index/position.
     */
    public @Nullable String getLocalName(int index, int position) {
        Local local = getLocal(index, position);
        if (local == null) return null;
        return local.name();
    }

    /**
     * Gets a local var descriptor based on the index and position.
     *
     * @param index
     *                 the index of the local var
     * @param position
     *                 the position in code where it was used
     *
     * @return the descriptor of the local, or {@code null} if no var matches the index/position.
     */
    public @Nullable String getDescriptor(int index, int position) {
        Local local = getLocal(index, position);
        if (local == null) return null;
        return local.descriptor();
    }

    /**
     * Gets either a local or a parameter name based on the index and position.
     * If no local or parameter exists for the index/position a name will be provided based on the given mapper.
     *
     * @param index
     *                 the index of the local var
     * @param position
     *                 the position in code where it was used
     * @param missingNameMapper
     *                 the mapper to provide names for items not found in this model.
     *
     * @return the name of the local or parameter
     * @see #getLocalName(int, int) For only local var name lookups, minus the generation of a generic names for missing vars.
     * @see #getParameterName(int) For only parameter var name lookups, minus the generation of generic names for missing vars.
     */
    public @NotNull String computeName(int index, int position, @NotNull IntFunction<String> missingNameMapper) {
        var name = getLocalName(index, position);
        if (name == null) {
            name = getParameterName(index);
        }
        if (name == null) {
            name = missingNameMapper.apply(index);
        }
        return name;
    }

    public record Local(int index, int start, int end, String name, String descriptor) {
        public boolean isPrimitive() {
            // Unintelligent check but will suffice
            return descriptor.length() == 1;
        }
    }

}
