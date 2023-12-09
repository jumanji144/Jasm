package me.darknet.assembler.helper;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

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
public record Names(@NotNull Map<Integer, String> parameters, @NotNull List<Local> locals) {

    public String getParameterName(int index) {
        // there is no parameter name
        if (index >= parameters.size())
            return null;
        return parameters.get(index);
    }

    public String getLocalName(int index, int position) {
        for (var local : locals) {
            if (local.index == index && local.start <= position && local.end >= position) {
                return local.name;
            }
        }
        return null;
    }

    /**
     * Gets either a local or a parameter name based on the index and position
     *
     * @param index
     *                 the index of the local or parameter
     * @param position
     *                 the position in code where it was used
     *
     * @return the name of the local or parameter
     */
    public String getName(int index, int position) {
        var name = getLocalName(index, position);
        if (name == null) {
            name = getParameterName(index);
        }
        if (name == null) {
            name = "v" + index;
        }
        return name;
    }

    public int getLocalIndex(String name, int position) {
        for (var local : locals) {
            if (local.name.equals(name) && local.start <= position && local.end >= position) {
                return local.index;
            }
        }
        return -1;
    }

    public int getLocalIndexFlat(String name) {
        for (var local : locals) {
            if (local.name.equals(name)) {
                return local.index;
            }
        }
        return -1;
    }

    public int getParameterIndex(String name) {
        for (var entry : parameters.entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public int getIndex(String name, int position) {
        var index = getLocalIndex(name, position);
        if (index == -1) {
            index = getParameterIndex(name);
        }
        // can't assume that the format is <v><index> so we can't just remove the v
        return -1;
    }

    public record Local(int index, int start, int end, String name, String descriptor) {
    }

}
