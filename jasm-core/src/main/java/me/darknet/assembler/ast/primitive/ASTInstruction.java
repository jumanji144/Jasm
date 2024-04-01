package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.CollectionUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ASTInstruction extends ASTElement {

    private final ASTIdentifier identifier;
    private final List<@Nullable ASTElement> arguments;

    public ASTInstruction(ASTIdentifier identifier, List<@Nullable ASTElement> arguments) {
        super(ElementType.CODE_INSTRUCTION, CollectionUtil.mergeNonNull(arguments, identifier));
        this.identifier = identifier;
        this.arguments = arguments;
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

    @SuppressWarnings("unchecked")
    public <T extends ASTElement> T argument(int index, Class<T> type) {
        ASTElement element = arguments.get(index);
        if (element instanceof ASTEmpty) {
            if (type == ASTArray.class)
                return (T) ASTEmpty.EMPTY_ARRAY;
            if (type == ASTObject.class)
                return (T) ASTEmpty.EMPTY_OBJECT;
        }
        if (element instanceof ASTNumber number && type == ASTIdentifier.class) {
            return (T) new ASTIdentifier(number.value());
        }
        return (T) arguments.get(index);
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
