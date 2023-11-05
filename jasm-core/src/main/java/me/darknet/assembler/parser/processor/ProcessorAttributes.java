package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.*;

import java.util.ArrayList;
import java.util.List;

public class ProcessorAttributes {

    // generic attributes
    public final List<ASTAnnotation> annotations = new ArrayList<>();
    public ASTString signature;

    // class attributes
    public ASTIdentifier superName;
    public ASTString sourceFile;
    public final List<ASTIdentifier> interfaces = new ArrayList<>();
    public final List<ASTInner> inners = new ArrayList<>();
    public final List<ASTIdentifier> nestMembers = new ArrayList<>();
    public final List<ASTIdentifier> nestHosts = new ArrayList<>();

    // all attributes
    List<ASTElement> attributes = new ArrayList<>();

    public void fill(ASTElement element) {
        if (!annotations.isEmpty() && element instanceof ASTAnnotated annotated) {
            annotated.setAnnotations(annotations);
        }

        if (signature != null && element instanceof ASTSigned signed) {
            signed.setSignature(signature);
        }

        if (  element instanceof ASTClass clazz) {
            if (sourceFile != null)clazz.setSourceFile(sourceFile);
            if (superName != null)clazz.setSuperName(superName);
            if (!interfaces.isEmpty())clazz.setInterfaces(interfaces);
            if (!inners.isEmpty()) clazz.setInnerClasses(inners);
        }
    }
}
