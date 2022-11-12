package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Arrays;

@Getter
public class MethodDeclarationGroup extends Group {
	private final AccessModsGroup accessMods;
	private final IdentifierGroup name;
	private final MethodParametersGroup params;
	private final String returnType;
	private final BodyGroup body;

	public MethodDeclarationGroup(Token value, AccessModsGroup accessMods, IdentifierGroup name, MethodParametersGroup params, String returnType, BodyGroup body) {
		super(GroupType.METHOD_DECLARATION, value, Arrays.asList(accessMods, name, params, body));
		this.accessMods = accessMods;
		this.params = params;
		this.name = name;
		// returnType is inferred from the descriptor
		this.returnType = returnType;
		this.body = body;
	}

	public String buildDescriptor() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i = 0; i < params.getMethodParameters().size(); i++) {
			sb.append(params.getMethodParameters().get(i).getDescriptorValue());
		}
		sb.append(")");
		sb.append(returnType);
		return sb.toString();
	}

}
