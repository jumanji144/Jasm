package me.darknet.assembler.parser.groups.method;

import lombok.Getter;
import me.darknet.assembler.parser.Group;

import java.util.Collections;
import java.util.List;

@Getter
public class MethodParametersGroup extends Group {

	private final List<MethodParameterGroup> methodParameters;

	public MethodParametersGroup() {
		this(Collections.emptyList());
	}

	public MethodParametersGroup(List<MethodParameterGroup> children) {
		super(GroupType.METHOD_PARAMETERS, children);
		methodParameters = children;
	}
}
