package me.darknet.assembler.compile.builder;

import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.MethodAnalysisLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JvmClassBuilder implements MethodAnalysisLookup {
    private ClassNode classNode = new ClassNode();

    // Linked to preserve method declaration order.
    private final Map<MethodNode, AnalysisResults> methodAnalysisResults = new LinkedHashMap<>();
    private final Set<String> modifiedMethodKeys = new LinkedHashSet<>();

    public @NotNull ClassNode node() {
        return classNode;
    }

    public void overlay(byte @NotNull [] classFile) {
        ClassNode node = new ClassNode();
        new ClassReader(classFile).accept(node, 0);
        classNode = node;
        modifiedMethodKeys.clear();
    }

    public void setVersion(int version) {
        classNode.version = 44 + version;
    }

    public void accessFlags(int accessFlags) {
        classNode.access = accessFlags;
        if ((accessFlags & Opcodes.ACC_RECORD) != 0 && classNode.recordComponents == null) {
            classNode.recordComponents = new ArrayList<>();
        }
    }

    public void type(@Nullable String internalName) {
        classNode.name = internalName;
    }

    public @Nullable String type() {
        return classNode.name;
    }

    public void signature(@Nullable String signature) {
        classNode.signature = signature;
    }

    public void setSuperClass(@Nullable String internalName) {
        classNode.superName = internalName;
    }

    public void addInterface(@NotNull String internalName) {
        if (classNode.interfaces == null)
            classNode.interfaces = new ArrayList<>();

        classNode.interfaces.add(internalName);
    }

    public void setSourceFile(@Nullable String sourceFile) {
        classNode.sourceFile = sourceFile;
    }

    public void setSourceDebugExtension(@Nullable String sourceDebugExtension) {
        classNode.sourceDebug = sourceDebugExtension;
    }

    public void setOuterClass(@Nullable String outerClass) {
        classNode.outerClass = outerClass;
    }

    public @Nullable String getOuterClass() {
        return classNode.outerClass;
    }

    public void setOuterMethod(@NotNull String owner, @NotNull String name, @NotNull String descriptor) {
        classNode.outerClass = owner;
        classNode.outerMethod = name;
        classNode.outerMethodDesc = descriptor;
    }

    public void addPermittedSubclass(@NotNull String internalName) {
        if (classNode.permittedSubclasses == null) {
            classNode.permittedSubclasses = new ArrayList<>();
        }
        classNode.permittedSubclasses.add(internalName);
    }

    public void setNestHost(@Nullable String internalName) {
        classNode.nestHostClass = internalName;
    }

    public void addNestMember(@NotNull String internalName) {
        if (classNode.nestMembers == null) {
            classNode.nestMembers = new ArrayList<>();
        }
        classNode.nestMembers.add(internalName);
    }

    public void addInnerClass(@NotNull InnerClassNode innerClass) {
        if (classNode.innerClasses == null) {
            classNode.innerClasses = new ArrayList<>();
        }
        classNode.innerClasses.add(innerClass);
    }

    public @NotNull RecordComponentNode putRecordComponent(@NotNull String name, @NotNull String descriptor,
                                                           @Nullable String signature) {
        if (classNode.recordComponents == null) {
            classNode.recordComponents = new ArrayList<>();
        }
        removeRecordComponent(name, descriptor);
        RecordComponentNode component = new RecordComponentNode(name, descriptor, signature);
        classNode.recordComponents.add(component);
        return component;
    }

    public @Nullable FieldNode field(@NotNull String name, @NotNull String descriptor) {
        return classNode.fields.stream()
                .filter(field -> field.name.equals(name) && field.desc.equals(descriptor))
                .findFirst()
                .orElse(null);
    }

    public @NotNull FieldNode putField(int accessFlags, @NotNull String name, @NotNull String descriptor) {
        FieldNode field = new FieldNode(accessFlags, name, descriptor, null, null);
        int index = findFieldIndex(name, descriptor);
        if (index >= 0) {
            classNode.fields.set(index, field);
        } else {
            classNode.fields.add(field);
        }
        return field;
    }

    public @Nullable MethodNode method(@NotNull String name, @NotNull String descriptor) {
        return classNode.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst()
                .orElse(null);
    }

    public @NotNull MethodNode putMethod(int accessFlags, @NotNull String name, @NotNull String descriptor) {
        MethodNode method = new MethodNode(accessFlags, name, descriptor, null, null);
        int index = findMethodIndex(name, descriptor);
        if (index >= 0) {
            classNode.methods.set(index, method);
        } else {
            classNode.methods.add(method);
        }
        markMethodModified(name, descriptor);
        return method;
    }

    public void markMethodModified(@NotNull String name, @NotNull String descriptor) {
        modifiedMethodKeys.add(methodKey(name, descriptor));
    }

    public @NotNull Set<String> modifiedMethodKeys() {
        return modifiedMethodKeys;
    }

    @NotNull
    public Map<MethodNode, AnalysisResults> getMethodAnalysisResults() {
        return methodAnalysisResults;
    }

    public void setMethodAnalysis(@NotNull String name, @NotNull String descriptor, @NotNull AnalysisResults results) {
        MethodNode method = method(name, descriptor);
        if (method == null) {
            method = new MethodNode(0, name, descriptor, null, null);
        }
        methodAnalysisResults.put(method, results);
    }

    public void setMethodAnalysis(@NotNull MethodNode method, @NotNull AnalysisResults results) {
        methodAnalysisResults.put(method, results);
    }

    @Override
    public @NotNull Map<MethodNode, AnalysisResults> allResults() {
        return methodAnalysisResults;
    }

    @Override
    public @Nullable AnalysisResults results(String name, String descriptor) {
        for (Map.Entry<MethodNode, AnalysisResults> entry : methodAnalysisResults.entrySet()) {
            MethodNode method = entry.getKey();
            if (method.name.equals(name) && method.desc.equals(descriptor)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public @Nullable AnalysisResults results(MethodNode method) {
        return methodAnalysisResults.get(method);
    }

    private int findFieldIndex(@NotNull String name, @NotNull String descriptor) {
        for (int i = 0; i < classNode.fields.size(); i++) {
            FieldNode field = classNode.fields.get(i);
            if (field.name.equals(name) && field.desc.equals(descriptor)) {
                return i;
            }
        }
        return -1;
    }

    private int findMethodIndex(@NotNull String name, @NotNull String descriptor) {
        for (int i = 0; i < classNode.methods.size(); i++) {
            MethodNode method = classNode.methods.get(i);
            if (method.name.equals(name) && method.desc.equals(descriptor)) {
                return i;
            }
        }
        return -1;
    }

    private void removeRecordComponent(@NotNull String name, @NotNull String descriptor) {
        List<RecordComponentNode> components = classNode.recordComponents;
        if (components == null) {
            return;
        }
        components.removeIf(component -> component.name.equals(name) && component.descriptor.equals(descriptor));
    }

    public static @NotNull String methodKey(@NotNull String name, @NotNull String descriptor) {
        return name + descriptor;
    }
}
