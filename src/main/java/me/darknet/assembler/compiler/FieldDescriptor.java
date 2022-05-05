package me.darknet.assembler.compiler;

import java.util.Arrays;

public class FieldDescriptor {

    public String name;
    public String owner;

    public FieldDescriptor(String desc) {
        String[] parts = desc.split("\\.");
        name = parts[0];
        if(parts.length > 1) {
            name = parts[1];
            owner = parts[0];
        }
    }

}
