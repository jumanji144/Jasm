package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTInner;

import java.util.ArrayList;
import java.util.List;

public class ProcessorAttributes {

    // generic attributes
    public final List<ASTAnnotation> annotations = new ArrayList<>();
    public ASTIdentifier signature;

    // class attributes
    public final List<ASTIdentifier> interfaces = new ArrayList<>();
    public ASTIdentifier superName;
    public final List<ASTInner> inners = new ArrayList<>();
    public ASTIdentifier sourceFile;
    public final List<ASTIdentifier> nestMembers = new ArrayList<>();
    public final List<ASTIdentifier> nestHosts = new ArrayList<>();

}
