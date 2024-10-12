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
import java.util.function.Consumer;

public class ProcessorList {

    private final List<ASTElement> result = new ArrayList<>();

    private ProcessorAttributes attributes = new ProcessorAttributes();

    public void add(ASTElement element) {
        result.add(element);
    }

    public List<ASTElement> getResult() {
        return result;
    }

    /**
     * Clears the generic attributes <i>(signature + annotations)</i> after use.
     *
     * @param attributesConsumer Consumer to accept the current attributes.
     */
    public void clearGeneric(Consumer<ProcessorAttributes> attributesConsumer) {
        attributesConsumer.accept(attributes);
        attributes.clearGenericAttributes();
    }

    /**
     * Yields a copy of the currently tracked attributes before clearing the internal state.
     *
     * @return Current attributes.
     */
    public ProcessorAttributes collectAttributes() {
        ProcessorAttributes attributes = this.attributes;
        this.attributes = new ProcessorAttributes();

        result.removeAll(attributes.attributes);

        return attributes;
    }

    private void addAttribute(ASTElement element) {
        attributes.attributes.add(element);
    }

    // mutators

    public void addVisibleAnnotation(ASTAnnotation annotation) {
        this.attributes.visibleAnnotations.add(annotation);
        addAttribute(annotation);
    }

    public void addInvisibleAnnotation(ASTAnnotation annotation) {
        this.attributes.invisibleAnnotations.add(annotation);
        addAttribute(annotation);
    }

    public void setSignature(ASTString signature) {
        this.attributes.signature = signature;
        addAttribute(signature);
    }

    public void addInterface(ASTIdentifier interfaceName) {
        this.attributes.interfaces.add(interfaceName);
        addAttribute(interfaceName);
    }

    public void setSuperName(ASTIdentifier superName) {
        this.attributes.superName = superName;
        addAttribute(superName);
    }

    public void setOuterClass(ASTElement className) {
        this.attributes.outerClass = className;
        addAttribute(className);
    }

    public void setOuterMethod(ASTOuterMethod outerMethod) {
        this.attributes.outerMethod = outerMethod;
        addAttribute(outerMethod);
    }

    public void addPermittedSubclass(ASTIdentifier subclassName) {
        this.attributes.permittedSubclasses.add(subclassName);
        addAttribute(subclassName);
    }


    public void addRecordComponent(ASTRecordComponent recordComponent) {
        this.attributes.recordComponents.add(recordComponent);
        addAttribute(recordComponent);
    }

    public void addInner(ASTInner inner) {
        this.attributes.inners.add(inner);
        addAttribute(inner);
    }

    public void setSourceFile(ASTString sourceFile) {
        this.attributes.sourceFile = sourceFile;
        addAttribute(sourceFile);
    }

    public void addNestMember(ASTIdentifier nestMember) {
        this.attributes.nestMembers.add(nestMember);
        addAttribute(nestMember);
    }

    public void setNestHost(ASTIdentifier nestHost) {
        this.attributes.nestHost = nestHost;
        addAttribute(nestHost);
    }

    public void removeAnnotation(ASTAnnotation value) {
        this.attributes.visibleAnnotations.remove(value);
        this.attributes.attributes.remove(value);
    }
}
