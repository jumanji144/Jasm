package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.visitor.Modifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Class declaration node containing class members and class-level metadata such as interfaces, nesting, and record components.
 */
public class ASTClass extends ASTMember {
    private final @NotNull List<ASTElement> contents;
    private @NotNull List<ASTIdentifier> interfaces = Collections.emptyList();
    private @NotNull List<ASTIdentifier> permittedSubclasses = Collections.emptyList();
    private @NotNull List<ASTRecordComponent> recordComponents = Collections.emptyList();
    private @NotNull List<ASTInner> inners = Collections.emptyList();
    private @Nullable ASTIdentifier superName;
    private @Nullable ASTNumber version;
    private @Nullable ASTString sourceFile;
    private @Nullable ASTString sourceDebugExtension;
    private @Nullable ASTElement outerClass;
    private @Nullable ASTOuterMethod outerMethod;
    private @Nullable ASTIdentifier nestHost;
    private @NotNull List<ASTIdentifier> nestMembers = Collections.emptyList();

    public ASTClass(@NotNull Modifiers modifiers, @NotNull ASTIdentifier name, @NotNull List<ASTElement> contents) {
        super(ElementType.CLASS, modifiers, name, name);
        List<ASTElement> ownedContents = CollectionUtil.immutableCopy(contents);
        addChildren(ownedContents);
        this.contents = ownedContents;
    }

    @Nullable
    public ASTString getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(@Nullable ASTString sourceFile) {
        replaceChild(this.sourceFile, sourceFile);
        this.sourceFile = sourceFile;
    }

    @Nullable
    public ASTString getSourceDebugExtension() {
        return sourceDebugExtension;
    }

    public void setSourceDebugExtension(@Nullable ASTString sourceDebugExtension) {
        replaceChild(this.sourceDebugExtension, sourceDebugExtension);
        this.sourceDebugExtension = sourceDebugExtension;
    }

    public @Nullable ASTIdentifier getSuperName() {
        return superName;
    }

    public void setSuperName(@Nullable ASTIdentifier superName) {
        replaceChild(this.superName, superName);
        this.superName = superName;
    }

    /**
     * @return Declared class-file version, or {@code null} when not present.
     */
    public @Nullable ASTNumber getVersion() {
        return version;
    }

    /**
     * @param version Declared class-file version, or {@code null} to clear it.
     */
    public void setVersion(@Nullable ASTNumber version) {
        replaceChild(this.version, version);
        this.version = version;
    }

    @Nullable
    public ASTElement getOuterClass() {
        return outerClass;
    }

    public void setOuterClass(@Nullable ASTElement outerClass) {
        replaceChild(this.outerClass, outerClass);
        this.outerClass = outerClass;
    }

    @Nullable
    public ASTOuterMethod getOuterMethod() {
        return outerMethod;
    }

    public void setOuterMethod(@Nullable ASTOuterMethod outerMethod) {
        replaceChild(this.outerMethod, outerMethod);
        this.outerMethod = outerMethod;
    }

    public void setNestHost(@Nullable ASTIdentifier nestHost) {
        replaceChild(this.nestHost, nestHost);
        this.nestHost = nestHost;
    }

    public @Nullable ASTIdentifier getNestHost() {
        return nestHost;
    }

    public void setNestMembers(@NotNull List<ASTIdentifier> nestMembers) {
        List<ASTIdentifier> ownedNestMembers = CollectionUtil.immutableCopy(nestMembers);
        replaceChildren(this.nestMembers, ownedNestMembers);
        this.nestMembers = ownedNestMembers;
    }

    public @NotNull List<ASTIdentifier> getNestMembers() {
        return nestMembers;
    }

    @NotNull
    public List<ASTIdentifier> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(@NotNull List<ASTIdentifier> interfaces) {
        List<ASTIdentifier> ownedInterfaces = CollectionUtil.immutableCopy(interfaces);
        replaceChildren(this.interfaces, ownedInterfaces);
        this.interfaces = ownedInterfaces;
    }

    public void setPermittedSubclasses(@NotNull List<ASTIdentifier> permittedSubclasses) {
        List<ASTIdentifier> ownedPermittedSubclasses = CollectionUtil.immutableCopy(permittedSubclasses);
        replaceChildren(this.permittedSubclasses, ownedPermittedSubclasses);
        this.permittedSubclasses = ownedPermittedSubclasses;
    }

    @NotNull
    public List<ASTRecordComponent> getRecordComponents() {
        return recordComponents;
    }

    public void setRecordComponents(@NotNull List<ASTRecordComponent> recordComponents) {
        List<ASTRecordComponent> ownedRecordComponents = CollectionUtil.immutableCopy(recordComponents);
        replaceChildren(this.recordComponents, ownedRecordComponents);
        this.recordComponents = ownedRecordComponents;
    }

    @NotNull
    public List<ASTIdentifier> getPermittedSubclasses() {
        return permittedSubclasses;
    }

    @NotNull
    public List<ASTInner> getInners() {
        return inners;
    }

    public void setInnerClasses(@NotNull List<ASTInner> inners) {
        List<ASTInner> ownedInners = CollectionUtil.immutableCopy(inners);
        replaceChildren(this.inners, ownedInners);
        this.inners = ownedInners;
    }

    @NotNull
    public List<ASTElement> contents() {
        return contents;
    }

    public @Nullable ASTElement content(int index) {
        return contents.get(index);
    }

}
