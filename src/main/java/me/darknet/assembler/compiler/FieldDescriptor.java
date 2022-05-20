package me.darknet.assembler.compiler;

import java.util.Arrays;

public class FieldDescriptor {

    public String name;
    public String owner;
    public String desc;

    public FieldDescriptor(String name, String desc) {
        if(name.contains(".")) {
            String[] parts = name.split("\\.");
            this.name = parts[0];
            if (parts.length > 1) {
                this.name = parts[1];
                owner = parts[0];
            }
        } else {
            int lastSlash = name.lastIndexOf("/");
            if (lastSlash != -1) {
                this.name = desc.substring(lastSlash + 1);
                owner = desc.substring(0, lastSlash);
            } else {
                this.name = desc;
            }
        }
        this.desc = desc;
    }

}
