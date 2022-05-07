package me.darknet.assembler.compiler;

public class MethodDescriptor {

    public String name;
    public String desc;
    public String owner;

    public MethodDescriptor(String desc) {
        this.name = desc.substring(0, desc.indexOf('('));
        this.desc = desc.substring(desc.indexOf('('));
        if(name.contains(".")){
            this.owner = name.substring(0, name.lastIndexOf('.'));
            this.name = name.substring(name.lastIndexOf('.')+1);
        } else if(name.contains("/")) {
            this.owner = name.substring(0, name.lastIndexOf('/'));
            this.name = name.substring(name.lastIndexOf('/')+1);
        }
    }

}
