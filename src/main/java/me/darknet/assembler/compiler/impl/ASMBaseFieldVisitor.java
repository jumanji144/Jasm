package me.darknet.assembler.compiler.impl;

import me.darknet.assembler.transform.FieldVisitor;

public class ASMBaseFieldVisitor implements FieldVisitor {

    private final org.objectweb.asm.FieldVisitor visitor;

    public ASMBaseFieldVisitor(org.objectweb.asm.FieldVisitor visitor) {
        this.visitor = visitor;
    }

    @Override
    public void visitEnd() {
        visitor.visitEnd();
    }
}
