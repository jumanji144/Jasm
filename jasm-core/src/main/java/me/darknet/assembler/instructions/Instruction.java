package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Registered instruction definition with validation, translation, and semantic metadata.
 *
 * @param <V>
 * 		Visitor type accepted by the translator.
 */
public class Instruction<V extends ASTInstructionVisitor> {

    final Operand[] operands;
    private final @Nullable String name;
    private final @Nullable BiConsumer<ASTInstruction, V> astTranslator;
    private final @Nullable BiConsumer<SemanticInstruction, V> semanticTranslator;
    private final @Nullable InstructionMetadata metadata;

    /**
     * Legacy constructor for callers that create an unregistered AST-translated instruction.
     */
    public Instruction(Operand[] operands, BiConsumer<ASTInstruction, V> translator) {
        this(null, operands, Objects.requireNonNull(translator, "translator"), null, null);
    }

    /**
     * Creates an instruction with target-neutral metadata and a semantic translator.
     */
    public Instruction(
            String name,
            Operand[] operands,
            BiConsumer<SemanticInstruction, V> translator,
            InstructionMetadata metadata) {
        this(Objects.requireNonNull(name, "name"), operands, null,
                Objects.requireNonNull(translator, "translator"), Objects.requireNonNull(metadata, "metadata"));
    }

    private Instruction(
            @Nullable String name,
            Operand[] operands,
            @Nullable BiConsumer<ASTInstruction, V> astTranslator,
            @Nullable BiConsumer<SemanticInstruction, V> semanticTranslator,
            @Nullable InstructionMetadata metadata) {
        this.name = name;
        this.operands = Objects.requireNonNull(operands, "operands").clone();
        this.astTranslator = astTranslator;
        this.semanticTranslator = semanticTranslator;
        this.metadata = metadata;
    }

    static <V extends ASTInstructionVisitor> Instruction<V> legacy(
            String name,
            Operand[] operands,
            BiConsumer<ASTInstruction, V> translator,
            InstructionMetadata metadata) {
        return new Instruction<>(Objects.requireNonNull(name, "name"), operands,
                Objects.requireNonNull(translator, "translator"), null, Objects.requireNonNull(metadata, "metadata"));
    }

    /**
     * @return Registered source mnemonic, or {@code null} for a directly constructed legacy instance.
     */
    public @Nullable String name() {
        return name;
    }

    /**
     * @return Semantic metadata, or {@code null} for a directly constructed legacy instance.
     */
    public @Nullable InstructionMetadata metadata() {
        return metadata;
    }

    /**
     * @return {@code true} when this definition carries {@code trait}.
     */
    public boolean hasTrait(@NotNull InstructionTrait trait) {
        return metadata != null && metadata.traits().contains(Objects.requireNonNull(trait, "trait"));
    }

    /**
     * @return Type operand index, or {@code -1} when no type role is present.
     */
    public int typeReferenceOperandIndex() {
        return operandIndex(OperandRole.RoleKind.TYPE);
    }

    /**
     * @return Variable operand index, or {@code -1} when no variable role is present.
     */
    public int variableOperandIndex() {
        return operandIndex(OperandRole.RoleKind.VARIABLE);
    }

    /**
     * @return Member-reference operand index, or {@code -1} when none is present.
     */
    public int memberReferenceOperandIndex() {
        return operandIndex(OperandRole.RoleKind.MEMBER);
    }

    /**
     * @return Switch shape, or {@code null} when this is not a switch instruction.
     */
    public @Nullable SwitchShape switchShape() {
        return metadata == null ? null : metadata.switchShape();
    }

    /**
     * @return Canonical source mnemonic for this definition.
     */
    public @NotNull String canonicalName() {
        if (metadata != null)
            return metadata.canonicalName();
        return Objects.requireNonNull(name, "Unregistered legacy instruction has no canonical name");
    }

    /**
     * @return Target-owned lowering identity, or {@code null} for source-only forms.
     */
    public @Nullable InstructionLowering lowering() {
        return metadata == null ? null : metadata.lowering();
    }

    /**
     * @return {@code true} when this target can lower the instruction.
     */
    public boolean isAvailable() {
        return metadata != null && metadata.unavailableReason() == null;
    }

    /**
     * @return Reason this target cannot lower the instruction, or {@code null} when available.
     */
    public @Nullable String unavailableReason() {
        return metadata == null ? null : metadata.unavailableReason();
    }

    /**
     * @return Index of the first operand declaring {@code kind}, or {@code -1} when absent.
     */
    public int operandIndex(@NotNull OperandRole.RoleKind kind) {
        if (metadata == null)
            return -1;
        return metadata.roles().stream()
                .filter(role -> role.kind() == Objects.requireNonNull(kind, "kind"))
                .mapToInt(OperandRole::index)
                .findFirst()
                .orElse(-1);
    }

    /**
     * @return Number of declared operands.
     */
    public int operandCount() {
        return operands.length;
    }

    /**
     * @return Operand schema at {@code index}.
     */
    public @NotNull Operand operand(int index) {
        return operands[index];
    }

    /**
     * @return Role declared for {@code index}, or {@code null} when absent or out of range.
     */
    public @Nullable OperandRole role(int index) {
        if (index < 0 || index >= operands.length || metadata == null)
            return null;
        return metadata.roles().stream()
                .filter(role -> role.index() == index)
                .findFirst()
                .orElse(null);
    }

    /**
     * Verifies an instruction's operand count, presence, and source shapes.
     */
    public void verify(ASTInstruction instruction, ProcessorContext context) {
        if (instruction.arguments().size() != operands.length) {
            context.throwError(
                    "Expected " + operands.length + " operands, got " + instruction.arguments().size(),
                    instruction.location()
            );
            return;
        }
        for (int i = 0; i < operands.length; i++) {
            @Nullable ASTElement arg = instruction.arguments().get(i);
            if (arg == null) {
                context.throwError("Expected operand " + i + " to be present", instruction.location());
                return;
            }
            operands[i].verify(context, arg);
        }
    }

    /**
     * Translates using the existing source-AST visitor path.
     */
    @SuppressWarnings("unchecked")
    public void transform(ASTInstruction instruction, ASTInstructionVisitor visitor) {
        if (astTranslator == null)
            throw new IllegalStateException("Instruction has no AST translator");
        astTranslator.accept(instruction, (V) visitor);
    }

    /**
     * Translates a validated semantic instruction to the visitor's target representation.
     */
    @SuppressWarnings("unchecked")
    public void transform(SemanticInstruction instruction, ASTInstructionVisitor visitor) {
        if (semanticTranslator == null)
            throw new IllegalStateException("Instruction has no semantic translator");
        semanticTranslator.accept(instruction, (V) visitor);
    }
}
