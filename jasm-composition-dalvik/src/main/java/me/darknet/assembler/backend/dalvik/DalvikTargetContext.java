package me.darknet.assembler.backend.dalvik;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.backend.dalvik.instructions.DalvikMethodData;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.backend.dalvik.instructions.DalvikInstructions;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.processing.MethodTargetData;
import me.darknet.assembler.target.AnnotationCapabilities;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.MethodAttributeParser;
import me.darknet.assembler.target.MethodAttributeRegistry;
import me.darknet.assembler.target.TargetContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Processing services owned by the Dalvik backend.
 */
public final class DalvikTargetContext implements TargetContext {
    public static final DalvikTargetContext INSTANCE = new DalvikTargetContext();

    private static final AnnotationCapabilities ANNOTATION_CAPABILITIES =
            capability -> capability != AnnotationCapability.TYPE_ANNOTATIONS
                            && capability != AnnotationCapability.PARAMETER_ANNOTATIONS;
    private static final MethodAttributeRegistry METHOD_ATTRIBUTES = createMethodAttributes();

    private DalvikTargetContext() {}

    private static MethodAttributeRegistry createMethodAttributes() {
        MethodAttributeRegistry registry = new MethodAttributeRegistry();
        registry.register(new MethodAttributeParser() {
            @Override
            public @NotNull String key() {
                return "registers";
            }

            @Override
            public @Nullable MethodTargetData parse(
                    @NotNull ProcessorContext context,
                    @Nullable ASTElement value,
                    @NotNull ASTElement declaration) {
                ASTNumber source = context.validateElement(
                        value, ElementType.NUMBER, "method register count", declaration
                );
                if (source == null)
                    return null;

                long count;
                try {
                    if (source.isFloatingPoint()) {
                        context.throwError(
                                DiagnosticCode.MALFORMED_DECLARATION,
                                "Method register count must be a nonnegative integer",
                                source.location()
                        );
                        return null;
                    }
                    count = source.asLong();
                } catch (NumberFormatException exception) {
                    context.throwError(
                            DiagnosticCode.MALFORMED_DECLARATION,
                            "Method register count must be a nonnegative integer",
                            source.location()
                    );
                    return null;
                }

                if (count < 0 || count > Integer.MAX_VALUE) {
                    context.throwError(
                            DiagnosticCode.MALFORMED_DECLARATION,
                            "Method register count must be a nonnegative integer",
                            source.location()
                    );
                    return null;
                }
                return new DalvikMethodData((int) count, source);
            }
        });
        return registry;
    }

    @Override
    public @NotNull Instructions<?> instructions() {
        return DalvikInstructions.INSTANCE;
    }

    @Override
    public @NotNull MethodAttributeRegistry methodAttributes() {
        return METHOD_ATTRIBUTES;
    }

    @Override
    public @NotNull AnnotationCapabilities annotationCapabilities() {
        return ANNOTATION_CAPABILITIES;
    }
}
