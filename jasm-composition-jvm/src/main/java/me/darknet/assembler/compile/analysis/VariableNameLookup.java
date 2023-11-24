package me.darknet.assembler.compile.analysis;

import org.jetbrains.annotations.NotNull;

/**
 * Basic index-to-name lookup for variable containers.
 */
public interface VariableNameLookup {
	/**
	 * @param index
	 * 		Index of variable.
	 *
	 * @return Name of variable. Should never be {@code null}.
	 * If the index does not have a known name, a generic name will be created.
	 */
	@NotNull
	String getVarName(int index);
}
