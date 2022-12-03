package me.darknet.assembler.parser.groups.declaration;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.attributes.AccessModsGroup;
import me.darknet.assembler.parser.groups.BodyGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.method.MethodParametersGroup;

@Getter
public class MethodDeclarationGroup extends Group {
	private final AccessModsGroup accessMods;
	private final IdentifierGroup name;
	private final MethodParametersGroup params;
	private final String returnType;
	private final BodyGroup body;

	public MethodDeclarationGroup(Token value, AccessModsGroup accessMods, IdentifierGroup name, MethodParametersGroup params, String returnType, BodyGroup body) {
		super(GroupType.METHOD_DECLARATION, value, accessMods, name, params, body);
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
