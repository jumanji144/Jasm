package me.darknet.assembler.printer;

import java.util.List;
import java.util.Map;

/**
 * Container for containing method variables aka. locals
 */
public class Names {

	public static class Local {
		public int index;
		public int start, end;
		public String name;
	}

	/**
	 * Map of parameter index to name
	 * <p>
	 * They must be the names given to each parameter name of the method
	 * defined in either the <a href="https://docs.oracle.com/javase/specs/jvms/se16/html/jvms-4.html#jvms-4.7.24">MethodParameters</a>
	 * or be deduced via the <a href="https://docs.oracle.com/javase/specs/jvms/se16/html/jvms-4.html#jvms-4.7.13">LocalVariableTable</a>
	 * if another variable in the LVT re-uses the same index as the parameter use the first name given to the parameter
	 * in this order:
	 * <ol>
	 *     <li>MethodParameters</li>
	 *     <li>LocalVariableTable</li>
	 *     		<ol>
	 *     		 <li>Use the first name that has the same type and index</li>
	 *     		</ol>
	 *     <li>Use a placeholder name p[n]</li>
	 * </ol>
	 */
	private final Map<Integer, String> parameters;
	/**
	 * A full list of all locals in the method
	 */
	private final List<Local> locals;

	public Names(Map<Integer, String> parameters, List<Local> locals) {
		this.parameters = parameters;
		this.locals = locals;
	}

	public Map<Integer, String> getParameters() {
		return parameters;
	}

	public List<Local> getLocals() {
		return locals;
	}

	public String getParameterName(int index) {
		// there is no parameter name
		if(index >= parameters.size())
			return null;
		return parameters.get(index);
	}

	public String getLocalName(int index, int position) {
		for(var local : locals) {
			if(local.index == index && local.start <= position && local.end >= position) {
				return local.name;
			}
		}
		return null;
	}

	/**
	 * Gets either a local or a parameter name based on the index and position
	 * @param index the index of the local or parameter
	 * @param position the position in code where it was used
	 * @return the name of the local or parameter
	 */
	public String getName(int index, int position) {
		var name = getLocalName(index, position);
		if(name == null) {
			name = getParameterName(index);
		}
		if(name == null) {
			name = "v" + index;
		}
		return name;
	}



}
