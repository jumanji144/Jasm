package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProcessorAttributes {

    // generic attributes
    public final List<ASTAnnotation> visibleAnnotations = new ArrayList<>();
    public final List<ASTAnnotation> invisibleAnnotations = new ArrayList<>();
    public final List<ASTAnnotation> visibleTypeAnnotations = new ArrayList<>();
    public final List<ASTAnnotation> invisibleTypeAnnotations = new ArrayList<>();
    public ASTString signature;

    // class attributes
    public ASTIdentifier superName;
    public ASTString sourceFile;
    public ASTOuterMethod outerMethod;
    public ASTElement outerClass;
    public ASTIdentifier nestHost;
    public final List<ASTIdentifier> nestMembers = new ArrayList<>();
    public final List<ASTIdentifier> interfaces = new ArrayList<>();
    public final List<ASTInner> inners = new ArrayList<>();
    public final List<ASTIdentifier> permittedSubclasses = new ArrayList<>();
    public final List<ASTRecordComponent> recordComponents = new ArrayList<>();

    // all attributes
    List<ASTElement> attributes = new ArrayList<>();

    public @NotNull ProcessorAttributes clearGenericAttributes() {
        signature = null;
        visibleAnnotations.clear();
        invisibleAnnotations.clear();
        visibleTypeAnnotations.clear();
        invisibleTypeAnnotations.clear();
        return this;
    }

    public void fill(@NotNull ASTElement element) {
        // Pass along a copy of the annotations so that we can clear our list reference and not affect
        // the annotated consumer.
        if (!visibleAnnotations.isEmpty() && element instanceof ASTAnnotated annotated)
            annotated.setVisibleAnnotations(new ArrayList<>(visibleAnnotations));
        if (!invisibleAnnotations.isEmpty() && element instanceof ASTAnnotated annotated)
            annotated.setInvisibleAnnotations(new ArrayList<>(invisibleAnnotations));
        if (!visibleTypeAnnotations.isEmpty() && element instanceof ASTAnnotated annotated)
            annotated.setVisibleTypeAnnotations(new ArrayList<>(visibleTypeAnnotations));
        if (!invisibleTypeAnnotations.isEmpty() && element instanceof ASTAnnotated annotated)
            annotated.setInvisibleTypeAnnotations(new ArrayList<>(invisibleTypeAnnotations));

        // Same idea as annotations above
        if (signature != null && element instanceof ASTSigned signed)
            signed.setSignature(signature);

        if (element instanceof ASTClass clazz) {
            if (sourceFile != null)
                clazz.setSourceFile(sourceFile);
            if (superName != null)
                clazz.setSuperName(superName);
            if (outerClass != null)
                clazz.setOuterClass(outerClass);
            if (outerMethod != null)
                clazz.setOuterMethod(outerMethod);
            if (nestHost != null)
                clazz.setNestHost(nestHost);
            if (!nestMembers.isEmpty())
                clazz.setNestMembers(nestMembers);
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
