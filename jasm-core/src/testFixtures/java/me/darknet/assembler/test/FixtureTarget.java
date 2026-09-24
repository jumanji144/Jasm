package me.darknet.assembler.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.processing.MethodTargetData;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.SwitchShape;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.target.AnnotationCapabilities;
import me.darknet.assembler.target.AnnotationCapability;
import me.darknet.assembler.target.AssemblyTarget;
import me.darknet.assembler.target.MethodAttributeParser;
import me.darknet.assembler.target.MethodAttributeRegistry;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.target.TargetId;
import me.darknet.assembler.visitor.ASTInstructionVisitor;

import java.util.EnumSet;
import java.util.List;

/**
 * Minimal target implementations used by core structural and parser tests.
 */
public final class FixtureTarget implements AssemblyTarget {
    public static final FixtureTarget JVM = new FixtureTarget("JVM", false);
    public static final FixtureTarget DALVIK = new FixtureTarget("DALVIK", true);

    private final TargetId id;
    private final TargetContext context;

    private FixtureTarget(String id, boolean dalvik) {
        this.id = new TargetId(id);
        this.context = new FixtureContext(dalvik);
    }

    @Override
    public TargetId id() {
        return id;
    }

    @Override
    public String displayName() {
        return id.value();
    }

    @Override
    public TargetContext context() {
        return context;
    }

    private static final class FixtureContext implements TargetContext {
        private final FixtureInstructions instructions;
        private final MethodAttributeRegistry methodAttributes;
        private final AnnotationCapabilities annotationCapabilities;

        private FixtureContext(boolean dalvik) {
            instructions = new FixtureInstructions(dalvik);
            methodAttributes = dalvik ? dalvikAttributes() : MethodAttributeRegistry.empty();
            annotationCapabilities = capability -> dalvik
                    ? capability != AnnotationCapability.TYPE_ANNOTATIONS
                    : capability != AnnotationCapability.SYSTEM_VISIBILITY;
        }

        @Override
        public Instructions<?> instructions() {
            return instructions;
        }

        @Override
        public MethodAttributeRegistry methodAttributes() {
            return methodAttributes;
        }

        @Override
        public AnnotationCapabilities annotationCapabilities() {
            return annotationCapabilities;
        }

        private static MethodAttributeRegistry dalvikAttributes() {
            MethodAttributeRegistry registry = new MethodAttributeRegistry();
            registry.register(new MethodAttributeParser() {
                @Override
                public String key() {
                    return "registers";
                }

                @Override
                public MethodTargetData parse(
                        ProcessorContext context,
                        ASTElement value,
                        ASTElement declaration) {
                    ASTNumber source = context.validateElement(value, ElementType.NUMBER, "method register count", declaration);
                    if (source == null)
                        return null;
                    if (source.isFloatingPoint()) {
                        context.throwError("Method register count must be a nonnegative integer", source.location());
                        return null;
                    }
                    long count;
                    try {
                        count = source.asLong();
                    } catch (NumberFormatException exception) {
                        context.throwError("Method register count must be a nonnegative integer", source.location());
                        return null;
                    }
                    if (count < 0 || count > Integer.MAX_VALUE) {
                        context.throwError("Method register count must be a nonnegative integer", source.location());
                        return null;
                    }
                    return new FixtureMethodData((int) count, source);
                }
            });
            return registry;
        }
    }

    private static final class FixtureInstructions extends Instructions<ASTInstructionVisitor> {
        private final boolean dalvik;

        private FixtureInstructions(boolean dalvik) {
            super(false);
            this.dalvik = dalvik;
            registerInstructions();
            freeze();
        }

        @Override
        protected void registerInstructions() {
            if (dalvik)
                registerDalvik();
            else
                registerJvm();
        }

        private void registerJvm() {
            registerFlexible("nop", 0);
            registerFlexible("aconst_null", 0);
            register("line", new Operand[]{new Operand((context, element) -> {})},
                    (instruction, visitor) -> {}, sourceMetadata(EnumSet.noneOf(InstructionTrait.class), List.of(), null, "line"));
            registerFlexible("return", 0, EnumSet.of(InstructionTrait.RETURN),
                    List.of(), null, "return");
            registerFlexible("athrow", 0, EnumSet.of(InstructionTrait.THROW),
                    List.of(), null, "athrow");
            registerFlexible("swap", 0);
            registerFlexible("ldc", 1);
            registerFlexible("ldc_w", 1, "ldc");
            registerFlexible("ldc2_w", 1, "ldc");
            for (String name : List.of("getstatic", "putstatic", "getfield", "putfield"))
                registerFlexible(name, 2, EnumSet.of(InstructionTrait.FIELD_REFERENCE, InstructionTrait.FALLTHROUGH),
                        List.of(memberRole(0)), null, name);
            for (String name : List.of("invokevirtual", "invokespecial", "invokestatic", "invokeinterface"))
                registerFlexible(name, 2, EnumSet.of(InstructionTrait.INVOKE, InstructionTrait.METHOD_REFERENCE,
                                InstructionTrait.FALLTHROUGH), List.of(memberRole(0)), null, name);
            registerFlexible("invokedynamic", 4);
            registerFlexible("tableswitch", 1, EnumSet.of(InstructionTrait.SWITCH),
                    List.of(new OperandRole(0, OperandRole.RoleKind.SWITCH_PAYLOAD, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.PAYLOAD)), SwitchShape.TABLE, "tableswitch");
            registerFlexible("lookupswitch", 1, EnumSet.of(InstructionTrait.SWITCH),
                    List.of(new OperandRole(0, OperandRole.RoleKind.SWITCH_PAYLOAD, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.PAYLOAD)), SwitchShape.LOOKUP, "lookupswitch");
            registerFlexible("multianewarray", 2, EnumSet.of(InstructionTrait.TYPE_REFERENCE),
                    List.of(new OperandRole(0, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "multianewarray");
            for (String name : List.of("iload", "lload", "fload", "dload", "aload", "ret"))
                registerFlexible(name, 1, EnumSet.of(InstructionTrait.VARIABLE_READ),
                        List.of(new OperandRole(0, OperandRole.RoleKind.VARIABLE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.NONE)), null, name);
            for (String name : List.of("istore", "lstore", "fstore", "dstore", "astore"))
                registerFlexible(name, 1, EnumSet.of(InstructionTrait.VARIABLE_WRITE),
                        List.of(new OperandRole(0, OperandRole.RoleKind.VARIABLE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.NONE)), null, name);
            registerFlexible("iinc", 2, EnumSet.of(InstructionTrait.VARIABLE_INCREMENT),
                    List.of(new OperandRole(0, OperandRole.RoleKind.VARIABLE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.NONE)), null, "iinc");
            for (String name : List.of("goto", "goto_w", "jsr", "jsr_w"))
                registerFlexible(name, 1, EnumSet.of(InstructionTrait.UNCONDITIONAL_BRANCH),
                        List.of(new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null,
                        name.endsWith("_w") ? name.substring(0, name.length() - 2) : name);
            for (String name : List.of("ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle"))
                registerFlexible(name, 1, EnumSet.of(InstructionTrait.CONDITIONAL_BRANCH, InstructionTrait.FALLTHROUGH),
                        List.of(new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, name);
            for (String name : List.of("new", "anewarray", "checkcast", "instanceof"))
                registerFlexible(name, 1, EnumSet.of(InstructionTrait.TYPE_REFERENCE),
                        List.of(new OperandRole(0, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, name);
        }

        private void registerDalvik() {
            register("line", new Operand[]{new Operand((context, element) -> {})},
                    (instruction, visitor) -> {}, sourceMetadata(EnumSet.noneOf(InstructionTrait.class), List.of(), null, "line"));
            for (String name : List.of("nop", "return-void", "return", "return-wide", "return-object", "move",
                    "move-wide", "move-object", "move-result", "move-result-wide", "move-result-object"))
                registerFlexible(name, name.startsWith("return-void") || "nop".equals(name) ? 0 : 1);
            registerFlexible("goto", 1, EnumSet.of(InstructionTrait.UNCONDITIONAL_BRANCH),
                    List.of(new OperandRole(0, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, "goto");
            for (String name : List.of("if-eq", "if-ne", "if-lt", "if-ge", "if-gt", "if-le"))
                registerFlexible(name, 3, EnumSet.of(InstructionTrait.CONDITIONAL_BRANCH, InstructionTrait.FALLTHROUGH),
                        List.of(new OperandRole(2, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, name);
            for (String name : List.of("if-eqz", "if-nez", "if-ltz", "if-gez", "if-gtz", "if-lez"))
                registerFlexible(name, 2, EnumSet.of(InstructionTrait.CONDITIONAL_BRANCH, InstructionTrait.FALLTHROUGH),
                        List.of(new OperandRole(1, OperandRole.RoleKind.LABEL, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.LABEL)), null, name);
            registerFlexible("packed-switch", 2, EnumSet.of(InstructionTrait.SWITCH),
                    List.of(new OperandRole(1, OperandRole.RoleKind.SWITCH_PAYLOAD, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.PAYLOAD)), SwitchShape.PACKED, "packed-switch");
            registerFlexible("sparse-switch", 2, EnumSet.of(InstructionTrait.SWITCH),
                    List.of(new OperandRole(1, OperandRole.RoleKind.SWITCH_PAYLOAD, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.PAYLOAD)), SwitchShape.SPARSE, "sparse-switch");
            registerFlexible("const-class", 2, EnumSet.of(InstructionTrait.TYPE_REFERENCE),
                    List.of(new OperandRole(1, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "const-class");
            for (String name : List.of("check-cast", "new-instance"))
                registerFlexible(name, 2, EnumSet.of(InstructionTrait.TYPE_REFERENCE),
                        List.of(new OperandRole(1, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, name);
            registerFlexible("instance-of", 3, EnumSet.of(InstructionTrait.TYPE_REFERENCE),
                    List.of(new OperandRole(2, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "instance-of");
            registerFlexible("new-array", 3, EnumSet.of(InstructionTrait.TYPE_REFERENCE),
                    List.of(new OperandRole(2, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, "new-array");
            for (String name : List.of("filled-new-array", "filled-new-array/range"))
                registerFlexible(name, 2, EnumSet.of(InstructionTrait.TYPE_REFERENCE),
                        List.of(new OperandRole(1, OperandRole.RoleKind.TYPE, OperandRole.WidthPolicy.SINGLE, OperandRole.ReferencePolicy.TYPE)), null, name);
        }

        /**
         * @param index
         * 		Operand position holding the member reference.
         *
         * @return Role declaring that operand as a member reference.
         */
        private static OperandRole memberRole(int index) {
            return new OperandRole(index, OperandRole.RoleKind.MEMBER, OperandRole.WidthPolicy.SINGLE,
                    OperandRole.ReferencePolicy.MEMBER);
        }

        private void registerFlexible(String name, int operandCount) {
            registerFlexible(name, operandCount, EnumSet.noneOf(InstructionTrait.class), List.of(), null, name);
        }

        private void registerFlexible(String name, int operandCount, String canonicalName) {
            registerFlexible(name, operandCount, EnumSet.noneOf(InstructionTrait.class), List.of(), null, canonicalName);
        }

        private void registerFlexible(String name, int operandCount, EnumSet<InstructionTrait> traits,
                                      List<OperandRole> roles, SwitchShape shape, String canonicalName) {
            Operand[] operands = new Operand[operandCount];
            for (int i = 0; i < operandCount; i++)
                operands[i] = new Operand((context, element) -> {});
            register(name, operands, (instruction, visitor) -> {},
                    metadata(traits, roles, shape, canonicalName, new FixtureLowering(canonicalName)));
        }
    }

    private record FixtureMethodData(int registers, ASTNumber source) implements MethodTargetData {
    }
}