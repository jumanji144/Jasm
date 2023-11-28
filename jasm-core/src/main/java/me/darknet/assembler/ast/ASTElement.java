package me.darknet.assembler.ast;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.processor.ProcessorAttributes;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ASTElement {

    protected final List<ASTElement> children;
    protected ElementType type;
    protected ASTElement parent;
    protected Token value;

    public ASTElement(ElementType type) {
        this(type, Collections.emptyList());
    }

    public ASTElement(ElementType type, ASTElement... children) {
        this(type, Arrays.asList(children));
    }

    @SuppressWarnings("unchecked")
    public ASTElement(ElementType type, @NotNull List<? extends ASTElement> children) {
        for (ASTElement child : children) {
            if (child == null)
                throw new IllegalStateException("Cannot add null child to prent element");
            child.parent = this;
        }
        this.type = type;
        this.children = (List<ASTElement>) children;
    }

    protected void addChild(@NotNull ASTElement element) {
        children.add(element);
    }

    protected void addChildren(@NotNull Collection<? extends ASTElement> elements) {
        children.addAll(elements);
    }

    protected void removeChild(@NotNull ASTElement element) {
        children.remove(element);
    }

    protected void removeChildren(@NotNull Collection<? extends ASTElement> elements) {
        children.removeAll(elements);
    }

    /**
     * @return Raw text of this element. Escape sequences are not escaped.
     *
     * @see ASTIdentifier#literal() For escaped content.
     */
    @Contract(pure = true)
    public String content() {
        return value == null ? null : value.content();
    }

    @Nullable
    public Range range() {
        if (value == null) {
            for (ASTElement child : children) {
                Token childValue = child.value();
                if (childValue != null) {
                    if (children.size() == 1) return childValue.range();
                    else {
                        int start = childValue.range().start();
                        return new Range(start, start + content().length());
                    }
                }
            }
        }
        return null;
    }

    public Token value() {
        return value;
    }

    public ElementType type() {
        return type;
    }

    public ASTElement parent() {
        return parent;
    }

    public @NotNull List<ASTElement> children() {
        return children;
    }

    public @Nullable Location location() {
        if (value == null) {
            for (ASTElement child : children) {
                Location location = child.location();
                if (location != null)
                    return location;
            }
        } else {
            return value.location();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends ASTElement> T accept(ProcessorAttributes attributes) {
        attributes.fill(this);
        return (T) this;
    }
}
