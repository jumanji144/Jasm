package me.darknet.assembler.compiler.impl;

import lombok.Setter;
import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.List;

@Setter
public class CachedClass {

    public int version;
    public int access;
    public String fullyQualifiedName;
    public String superGroup;
    public List<String> implementsGroups = new ArrayList<>();

    public boolean hasBuilt;

    public void build(ClassVisitor cv) {
        cv.visit(version, access, fullyQualifiedName, null, superGroup, implementsGroups.toArray(new String[0]));
        hasBuilt = true;
    }

    public void addImplements(String group) {
        implementsGroups.add(group);
    }


}
