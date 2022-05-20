package me.darknet.assembler.compiler;

import me.darknet.assembler.parser.MethodParameter;
import org.objectweb.asm.Type;

public class MethodDescriptor {

    public String name;
    public String owner;
    public String returnType;
    public String descriptor;

    public MethodDescriptor(String name, String desc) {
        this.name = name;
        if(this.name.contains(".")) {
            this.owner = this.name.substring(0, this.name.lastIndexOf('.'));
            this.name = this.name.substring(this.name.lastIndexOf('.') + 1);
        }else {
            this.owner = this.name.substring(0, this.name.lastIndexOf('/'));
            this.name = this.name.substring(this.name.lastIndexOf('/') + 1);
        }
        this.descriptor = desc;
    }

    public String getDescriptor() {
        return this.descriptor;
    }

}
