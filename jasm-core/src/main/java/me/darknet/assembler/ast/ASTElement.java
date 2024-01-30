package me.darknet.assembler.ast;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.processor.ProcessorAttributes;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ASTElement {
    private static final Comparator<ASTElement> SORT_POS = (o1, o2) -> {
        Location l1 = o1.location();
        Location l2 = o2.location();
        return (l1 != null && l2 != null) ? l1.compareTo(l2) : 0;
    };
    protected final List<ASTElement> children;
    protected ElementType type;
    protected ASTElement parent;
    protected Token value;
    protected Range cachedRange;

    public ASTElement(ElementType type) {
        this(type, Collections.emptyList());
    }

    public ASTElement(ElementType type, ASTElement... children) {
        this(type, Arrays.asList(children));
    }

    public ASTElement(ElementType type, @NotNull List<? extends ASTElement> children) {
        this.children = children.stream().filter(Objects::nonNull).sorted(SORT_POS)
                .collect(Collectors.toCollection(ArrayList::new));
        for (ASTElement child : this.children) {
            child.parent = this;
        }
        this.type = type;
    }

    protected void addChild(@NotNull ASTElement element) {
        element.parent = this;
        children.add(element);
        children.sort(SORT_POS);
    }

    protected void addChildren(@NotNull Collection<? extends ASTElement> elements) {
        Collection<ASTElement> filtered = elements.stream().filter(Objects::nonNull).collect(Collectors.toList());
        filtered.forEach(element -> element.parent = this);
        children.addAll(filtered);
        children.sort(SORT_POS);
    }

    protected void removeChild(@NotNull ASTElement element) {
        children.remove(element);
    }

    protected void removeChildren(@NotNull Collection<? extends ASTElement> elements) {
        children.removeAll(elements);
    }

    protected void replaceChild(@Nullable ASTElement element, @NotNull ASTElement replacement) {
        if (element != null) removeChild(element);
        addChild(replacement);
    }

    protected void replaceChildren(@Nullable Collection<? extends ASTElement> elements, @NotNull List<? extends ASTElement> replacements) {
        if (elements != null) removeChildren(elements);
        addChildren(replacements);
    }

    /**
     * @return Raw text of this element. Escape sequences are not escaped.
     *
     * @see ASTIdentifier#literal() For escaped content.
     */
    @Nullable
    @Contract(pure = true)
    public String content() {
        return value == null ? null : value.content();
    }

    public Range range() {
        if (cachedRange != null)
            return cachedRange;
        if (value == null || children.size() > 1) {
            List<ASTElement> localChildren = children;
            if (localChildren.isEmpty()) // no possible range usable
                return Range.EMPTY;

            if (localChildren.size() == 1)
                return localChildren.get(0).range();

            Range first = localChildren.get(0).range();
            Range last = localChildren.get(localChildren.size() - 1).range();
            if (first == null || last == null)
                return Range.EMPTY;

            return cachedRange = new Range(first.start(), last.end());
        }
        return cachedRange = value.range();
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
