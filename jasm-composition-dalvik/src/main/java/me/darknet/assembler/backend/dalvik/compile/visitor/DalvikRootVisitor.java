package me.darknet.assembler.backend.dalvik.compile.visitor;

import me.darknet.assembler.backend.dalvik.DalvikModifiers;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.processing.ProcessedMethod;
import me.darknet.assembler.error.DiagnosticSink;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.visitor.*;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.FieldMember;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.type.ClassType;
import me.darknet.dex.tree.type.InstanceType;
import me.darknet.dex.tree.type.MethodType;
import me.darknet.dex.tree.type.Type;
import me.darknet.dex.tree.type.TypeParser;
import me.darknet.dex.tree.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Visits a top-level assembler declaration and builds the Dalvik {@link ClassDefinition} tree.
 */
public class DalvikRootVisitor implements ASTRootVisitor {
    private final DiagnosticSink sink;
    private ClassDefinition definition;

    /**
     * @param overlay
     * 		Class to build on, or {@code null} when the source declares the class itself.
     * @param sink
     * 		Sink reporting problems this backend cannot express in a dex tree.
     */
    public DalvikRootVisitor(@Nullable ClassDefinition overlay, @NotNull DiagnosticSink sink) {
        this.definition = overlay;
        this.sink = sink;
    }

    /**
     * @return The visited class definition. Only {@code null} if the source declares a top-level field or method without an overlay, which is not allowed.
     */
    public @Nullable ClassDefinition getDefinition() {
        return definition;
    }

    @Override
    public ASTAnnotationVisitor visitAnnotation(@NotNull ASTAnnotation annotation) {
        return null;
    }

    @Override
    public ASTClassVisitor visitClass(Modifiers modifiers, ASTIdentifier name) {
        int accessFlags = DalvikModifiers.getClassModifiers(modifiers);
        InstanceType type = Types.instanceTypeFromInternalName(name.literal());
        if (definition == null)
            definition = new ClassDefinition(type, null, accessFlags);
        return new DalvikClassVisitor(definition, sink);
    }

    @Override
    public ASTFieldVisitor visitField(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor) {
        ClassDefinition definition = requireDefinition(name.location());
        if (definition == null)
            return null;
        ClassType type = new TypeParser(descriptor.literal()).requireClassType();
        FieldMember member = new FieldMember(name.literal(), type, DalvikModifiers.getFieldModifiers(modifiers));
        definition.putField(member);
        return new DalvikFieldVisitor(member);
    }

    @Override
    public ASTMethodVisitor visitMethod(@NotNull ASTMethod source, @NotNull ProcessedMethod processed) {
        Modifiers modifiers = source.getModifiers();
        ASTIdentifier name = source.getName();
        ASTIdentifier descriptor = source.getDescriptor();
        ClassDefinition definition = requireDefinition(source.location());
        if (definition == null)
            return null;

        Type parsedType = new TypeParser(descriptor.literal()).required();
        if (!(parsedType instanceof MethodType methodType))
            throw new IllegalStateException("Expected method descriptor: " + descriptor.literal());

        MethodMember member = new MethodMember(name.literal(), methodType, DalvikModifiers.getMethodModifiers(modifiers));
        definition.putMethod(member);
        return new DalvikMethodVisitor(member, processed, sink);
    }

    /**
     * @param location
     * 		Location of the member that needs the class.
     *
     * @return The class being built, or {@code null} after reporting that no overlay was provided.
     */
    private @Nullable ClassDefinition requireDefinition(Location location) {
        if (definition == null) {
            // The compiler rejects this shape before traversal, so this only guards a caller that bypasses it.
            sink.error(DiagnosticCode.INVALID_EMISSION, "Top-level Dalvik members require an overlay class", location);
        }
        return definition;
    }
}
