package me.darknet.assembler.compiler.impl;

import lombok.Data;
import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.List;

@Data
public class CachedClass {
	private int version;
	private int access;
	private String fullyQualifiedName;
	private String superGroup = "java/lang/Object";
	private String signature;
	private List<String> implementsGroups = new ArrayList<>();

	private boolean built;

	public void build(ClassVisitor cv) {
		cv.visit(version, access, fullyQualifiedName, signature, superGroup, implementsGroups.toArray(new String[0]));
		built = true;
	}

	public void addImplements(String group) {
		implementsGroups.add(group);
	}


}
