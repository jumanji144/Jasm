package me.darknet.assembler.compiler;

import me.darknet.assembler.parser.MethodParameter;

public class MethodDescriptor {

    public String name;
    public String owner;
    public String returnType;
    public MethodParameter[] parameters = new MethodParameter[0];
    public String descriptor;

    public MethodDescriptor(String desc, boolean parseParameters) {
        this.name = desc.substring(0, desc.indexOf('('));
        if(name.contains(".")){
            this.owner = name.substring(0, name.lastIndexOf('.'));
            this.name = name.substring(name.lastIndexOf('.')+1);
        } else if(name.contains("/")) {
            this.owner = name.substring(0, name.lastIndexOf('/'));
            this.name = name.substring(name.lastIndexOf('/')+1);
        }
        this.returnType = desc.substring(desc.indexOf(')')+1);
        if(desc.indexOf('(') + 1 == desc.indexOf(')')) {
            this.descriptor = "()" + this.returnType;
        }else {
            if (parseParameters) {
                String params = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
                String[] paramTypes = params.split(",");
                this.parameters = new MethodParameter[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    String[] param = paramTypes[i].split(":");
                    this.parameters[i] = new MethodParameter(param[0], param[1]);
                }
            } else {
                this.descriptor = desc.substring(desc.indexOf('('));
            }
        }
    }

    public String getDescriptor() {
        if(descriptor != null) return this.descriptor;
        StringBuilder sb = new StringBuilder();
        sb.append('(');
            for (MethodParameter parameter : parameters) {
                sb.append(parameter.getDescriptor());
            }
        sb.append(')');
        sb.append(returnType);
        return sb.toString();
    }

}
