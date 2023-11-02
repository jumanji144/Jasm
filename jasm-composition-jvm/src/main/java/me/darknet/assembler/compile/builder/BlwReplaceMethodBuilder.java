package me.darknet.assembler.compile.builder;

import dev.xdark.blw.classfile.generic.GenericMethodBuilder;
import dev.xdark.blw.type.MethodType;

public class BlwReplaceMethodBuilder extends GenericMethodBuilder {
	public BlwReplaceMethodBuilder(int accessFlags, String name, MethodType type) {
		super(accessFlags, name, type);
	}
}
