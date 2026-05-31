package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTInner;
import me.darknet.assembler.ast.specific.ASTOuterMethod;
import me.darknet.assembler.ast.specific.ASTRecordComponent;

import java.util.ArrayList;
import java.util.List;

final class ProcessingState {
    private final List<ASTElement> result = new ArrayList<>();
    private ProcessorAttributes attributes = new ProcessorAttributes();

    void add(ASTElement element) {
        result.add(element);
    }

    List<ASTElement> getResult() {
        return result;
    }

    ProcessorAttributes collectAttributes() {
        ProcessorAttributes snapshot = attributes;
        attributes = new ProcessorAttributes();
        result.removeAll(snapshot.attributes);
        return snapshot;
    }

    ProcessorAttributes collectGenericAttributes() {
        ProcessorAttributes snapshot = new ProcessorAttributes();
        snapshot.signature = attributes.signature;
        snapshot.deprecated = attributes.deprecated;
        snapshot.deprecatedAttribute = attributes.deprecatedAttribute;
        snapshot.visibleAnnotations.addAll(attributes.visibleAnnotations);
        snapshot.invisibleAnnotations.addAll(attributes.invisibleAnnotations);
        snapshot.visibleTypeAnnotations.addAll(attributes.visibleTypeAnnotations);
        snapshot.invisibleTypeAnnotations.addAll(attributes.invisibleTypeAnnotations);
        if (snapshot.signature != null) {
            snapshot.attributes.add(snapshot.signature);
        }
        if (snapshot.deprecatedAttribute != null) {
            snapshot.attributes.add(snapshot.deprecatedAttribute);
        }
        snapshot.attributes.addAll(snapshot.visibleAnnotations);
        snapshot.attributes.addAll(snapshot.invisibleAnnotations);
        snapshot.attributes.addAll(snapshot.visibleTypeAnnotations);
        snapshot.attributes.addAll(snapshot.invisibleTypeAnnotations);

        result.removeAll(snapshot.attributes);
        attributes.attributes.removeAll(snapshot.attributes);
        attributes.clearGenericAttributes();
        return snapshot;
    }

    void addVisibleAnnotation(ASTAnnotation annotation) {
        attributes.visibleAnnotations.add(annotation);
        addAttribute(annotation);
    }

    void addInvisibleAnnotation(ASTAnnotation annotation) {
        attributes.invisibleAnnotations.add(annotation);
        addAttribute(annotation);
    }

    void addVisibleTypeAnnotation(ASTAnnotation annotation) {
        attributes.visibleTypeAnnotations.add(annotation);
        addAttribute(annotation);
    }

    void addInvisibleTypeAnnotation(ASTAnnotation annotation) {
        attributes.invisibleTypeAnnotations.add(annotation);
        addAttribute(annotation);
    }

    void setSignature(ASTString signature) {
        attributes.signature = signature;
        addAttribute(signature);
    }

    void addInterface(ASTIdentifier interfaceName) {
        attributes.interfaces.add(interfaceName);
        addAttribute(interfaceName);
    }

    void setSuperName(ASTIdentifier superName) {
        attributes.superName = superName;
        addAttribute(superName);
    }

    void setOuterClass(ASTElement className) {
        attributes.outerClass = className;
        addAttribute(className);
    }

    void setOuterMethod(ASTOuterMethod outerMethod) {
        attributes.outerMethod = outerMethod;
        addAttribute(outerMethod);
    }

    void addPermittedSubclass(ASTIdentifier subclassName) {
        attributes.permittedSubclasses.add(subclassName);
        addAttribute(subclassName);
    }

    void addRecordComponent(ASTRecordComponent recordComponent) {
        attributes.recordComponents.add(recordComponent);
        addAttribute(recordComponent);
    }

    void addInner(ASTInner inner) {
        attributes.inners.add(inner);
        addAttribute(inner);
    }

    void setSourceFile(ASTString sourceFile) {
        attributes.sourceFile = sourceFile;
        addAttribute(sourceFile);
    }

    void setSourceDebugExtension(ASTString sourceDebugExtension) {
        attributes.sourceDebugExtension = sourceDebugExtension;
        addAttribute(sourceDebugExtension);
    }

    void addNestMember(ASTIdentifier nestMember) {
        attributes.nestMembers.add(nestMember);
        addAttribute(nestMember);
    }

    void setNestHost(ASTIdentifier nestHost) {
        attributes.nestHost = nestHost;
        addAttribute(nestHost);
    }

    void setDeprecated(ASTElement deprecated) {
        attributes.deprecated = true;
        attributes.deprecatedAttribute = deprecated;
        addAttribute(deprecated);
    }

    private void addAttribute(ASTElement element) {
        attributes.attributes.add(element);
    }
}
