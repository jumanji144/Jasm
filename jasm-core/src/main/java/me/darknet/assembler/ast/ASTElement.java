package me.darknet.assembler.ast;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.processor.ProcessorAttributes;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
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

    protected void replaceChild(@Nullable ASTElement element, @Nullable ASTElement replacement) {
        if (element != null) removeChild(element);
        if (replacement != null) addChild(replacement);
    }

    protected void replaceChildren(@Nullable Collection<? extends ASTElement> elements,
                                   @Nullable List<? extends ASTElement> replacements) {
        if (elements != null) removeChildren(elements);
        if (replacements != null) addChildren(replacements);
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


    /**
     * Recursively walk the AST model.
     *
     * @param visitor Visitor that accepts the current element, then all children if the current element returns {@code true}.
     */
    public void walk(@NotNull Predicate<ASTElement> visitor) {
        if (visitor.test(this))
            children().forEach(c -> c.walk(visitor));
    }

    /**
     * Pick the deepest AST element at the given absolute position.
     *
     * @param position Position to pick an element of.
     * @return Deepest element at the given absolute position. If no child matches then this returns the current element.
     */
    @NotNull
    public ASTElement pick(int position) {
        for (ASTElement child : children()) {
            if (child.range().within(position)) {
                return child.pick(position);
            }
        }
        return this;
    }

    /**
     * @return Inclusive range of AST content.
     */
    public @NotNull Range range() {
        if (cachedRange != null)
            return cachedRange;
        if (value == null || children.size() > 1) {
            List<ASTElement> localChildren = children;
            if (localChildren.isEmpty()) // no possible range usable
                return Range.EMPTY;

            Range first = localChildren.get(0).range();
            if (localChildren.size() == 1)
                return createRange(first.start(), first.end());

            Range last = localChildren.get(localChildren.size() - 1).range();
            return cachedRange = createRange(first.start(), last.end());
        }
        Range range = value.range();
        return cachedRange = createRange(range.start(), range.end());
    }

    /**
     * Child types will override this if they need to adjust the range slightly.
     * Consider {@link ASTLabel} which normally would not include the ':' without
     * adding {@code end + 1}.
     *
     * @param start New range start.
     * @param end New range end.
     * @return New range.
     */
    protected @NotNull Range createRange(int start, int end) {
        return new Range(start, end);
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
