package me.darknet.assembler.compile.builder;

import dev.xdark.blw.classfile.generic.GenericFieldBuilder;
import dev.xdark.blw.type.ClassType;

public class BlwReplaceFieldBuilder extends GenericFieldBuilder  {
    public BlwReplaceFieldBuilder(int accessFlags, String name, ClassType type) {
        super(accessFlags, name, type);
    }
}
