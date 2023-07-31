package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTInner;

import java.util.ArrayList;
import java.util.List;

public class ProcessorList {

    private final List<ASTElement> result = new ArrayList<>();

    private ProcessorAttributes attributes = new ProcessorAttributes();

    private final List<Integer> attributeIndexes = new ArrayList<>();

    public void add(ASTElement element) {
        result.add(element);
    }

    public List<ASTElement> getResult() {
        return result;
    }

    public ProcessorAttributes collectAttributes() {
        ProcessorAttributes attributes = this.attributes;
        this.attributes = new ProcessorAttributes();

        for (int attributeIndex : attributeIndexes) {
            result.remove(attributeIndex);
        }

        attributeIndexes.clear();

        return attributes;
    }

    private void addAttribute() {
        attributeIndexes.add(result.size());
    }

    // mutators

    public void addAnnotation(ASTAnnotation annotation) {
        this.attributes.annotations.add(annotation);
    }

    public void setSignature(ASTIdentifier signature) {
        this.attributes.signature = signature;
    }

    public void addInterface(ASTIdentifier interfaceName) {
        this.attributes.interfaces.add(interfaceName);
    }

    public void setSuperName(ASTIdentifier superName) {
        this.attributes.superName = superName;
    }

    public void addInner(ASTInner inner) {
        this.attributes.inners.add(inner);
    }

    public void setSourceFile(ASTIdentifier sourceFile) {
        this.attributes.sourceFile = sourceFile;
    }

    public void addNestMember(ASTIdentifier nestMember) {
        this.attributes.nestMembers.add(nestMember);
    }

    public void addNestHost(ASTIdentifier nestHost) {
        this.attributes.nestHosts.add(nestHost);
    }

    public void removeAnnotation(ASTAnnotation value) {
        this.attributes.annotations.remove(value);
        this.attributeIndexes.remove((Integer) result.indexOf(value));
    }

}
