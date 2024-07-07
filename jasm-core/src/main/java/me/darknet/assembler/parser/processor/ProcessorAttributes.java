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
    public ASTOuterMethod outerMethod;
    public ASTElement outerClass;
    public final List<ASTIdentifier> interfaces = new ArrayList<>();
    public final List<ASTInner> inners = new ArrayList<>();
    public final List<ASTIdentifier> permittedSubclasses = new ArrayList<>();
    public final List<ASTRecordComponent> recordComponents = new ArrayList<>();
    public final List<ASTIdentifier> nestMembers = new ArrayList<>();
    public final List<ASTIdentifier> nestHosts = new ArrayList<>();

    // all attributes
    List<ASTElement> attributes = new ArrayList<>();

    public ProcessorAttributes clearGenericAttributes() {
        signature = null;
        annotations.clear();
        return this;
    }

    public void fill(ASTElement element) {
        if (!annotations.isEmpty() && element instanceof ASTAnnotated annotated) {
            // Pass along a copy of the annotations so that we can clear our list reference and not affect
            // the annotated consumer.
            annotated.setAnnotations(new ArrayList<>(annotations));
        }

        if (signature != null && element instanceof ASTSigned signed) {
            // Same idea as annotations above
            signed.setSignature(signature);
        }

        if (element instanceof ASTClass clazz) {
            if (sourceFile != null)
                clazz.setSourceFile(sourceFile);
            if (superName != null)
                clazz.setSuperName(superName);
            if (outerClass != null)
                clazz.setOuterClass(outerClass);
            if (outerMethod != null)
                clazz.setOuterMethod(outerMethod);
            if (!interfaces.isEmpty())
                clazz.setInterfaces(interfaces);
            if (!permittedSubclasses.isEmpty())
                clazz.setPermittedSubclasses(permittedSubclasses);
            if (!recordComponents.isEmpty())
                clazz.setRecordComponents(recordComponents);
            if (!inners.isEmpty())
                clazz.setInnerClasses(inners);
        }
    }
}
