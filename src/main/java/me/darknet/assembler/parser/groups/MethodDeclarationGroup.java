package me.darknet.assembler.parser.groups;

import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

public class MethodDeclarationGroup extends Group {

    public AccessModsGroup accessMods;
    public IdentifierGroup name;
    public MethodParametersGroup params;
    public String returnType;
    public BodyGroup body;

    public MethodDeclarationGroup(Token value, AccessModsGroup accessMods, IdentifierGroup name, MethodParametersGroup params, String returnType, BodyGroup body) {
        super(GroupType.METHOD_DECLARATION, value, accessMods, name, params, body);
        this.accessMods = accessMods;
        this.params = params;
        this.name = name;
        // returnType is inferred from the descriptor
        this.returnType = returnType;
        this.body = body;
    }

    public AccessModsGroup getAccessMods() {
        return accessMods;
    }

    public MethodParametersGroup getParams() {
        return params;
    }

    public BodyGroup getBody() {
        return body;
    }

    public IdentifierGroup getName() {
        return name;
    }

    public String buildDescriptor() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < params.methodParameters.length; i++) {
            sb.append(params.methodParameters[i].getDescriptorValue());
        }
        sb.append(")");
        sb.append(returnType);
        return sb.toString();
    }

}
