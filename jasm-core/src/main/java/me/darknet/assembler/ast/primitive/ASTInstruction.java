package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.CollectionUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Instruction node containing a mnemonic identifier and its source operands.
 */
public class ASTInstruction extends ASTElement {

    private final ASTIdentifier identifier;
    private final List<@Nullable ASTElement> arguments;

    public ASTInstruction(ASTIdentifier identifier, List<@Nullable ASTElement> arguments) {
        super(ElementType.CODE_INSTRUCTION, CollectionUtil.mergeNonNull(arguments, identifier));
        this.identifier = identifier;
        this.arguments = CollectionUtil.immutableCopy(arguments);
    }

    @Override
    public @NotNull String content() {
        if (arguments.isEmpty()) return identifier.content();
        return identifier.content() + " " + arguments.stream()
                .filter(Objects::nonNull)
                .map(ASTElement::content)
                .collect(Collectors.joining(" "));
    }

    public ASTIdentifier identifier() {
        return identifier;
    }

    public List<@Nullable ASTElement> arguments() {
        return arguments;
    }

    /**
     * Returns an operand as the requested AST type, preserving the existing empty-value and numeric-token
     * adaptations. A {@code null} operand is returned unchanged.
     *
     * @param index
     *        The operand index.
     * @param type
     *        The expected AST element type.
     * @param <T>
     *        The expected AST element type.
     * @return The operand, adapted when supported, or {@code null} when the operand is {@code null}.
     * @throws IllegalStateException if a non-null operand is incompatible with the requested type.
     */
    public <T extends ASTElement> T argument(int index, Class<T> type) {
        ASTElement element = arguments.get(index);
        if (element instanceof ASTEmpty) {
            if (type == ASTArray.class)
                element = ASTEmpty.EMPTY_ARRAY;
            else if (type == ASTObject.class)
                element = ASTEmpty.EMPTY_OBJECT;
        }
        if (element instanceof ASTNumber number && type == ASTIdentifier.class)
            element = new ASTIdentifier(number.value());
        if (element != null && !type.isInstance(element)) {
            throw new IllegalStateException("Operand " + index + " is " + element.getClass().getSimpleName()
                    + " but the translator expects " + type.getSimpleName());
        }
        return type.cast(element);
    }

    public ASTIdentifier argument(int index) {
        return argument(index, ASTIdentifier.class);
    }

    public ASTArray argumentArray(int index) {
        return argument(index, ASTArray.class);
    }

    public ASTObject argumentObject(int index) {
        return argument(index, ASTObject.class);
    }
}
