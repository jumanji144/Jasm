package me.darknet.assembler.compiler;

import java.util.Arrays;

public class FieldDescriptor {

    public String name;
    public String owner;
    public String desc;

    public FieldDescriptor(String desc) {
        if(desc.contains(".")) {
            String[] parts = desc.split("\\.");
            name = parts[0];
            if (parts.length > 1) {
                name = parts[1];
                owner = parts[0];
            }
        } else {
            int lastSlash = desc.lastIndexOf("/");
            if (lastSlash != -1) {
                name = desc.substring(lastSlash + 1);
                owner = desc.substring(0, lastSlash);
            } else {
                name = desc;
            }
        }
    }

}
