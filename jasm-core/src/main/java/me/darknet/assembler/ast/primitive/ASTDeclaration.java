package me.darknet.assembler.ast.primitive;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.util.CollectionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * AST element representing a declaration (.keyword) where it's children are the
 * elements of the declaration. A declaration can contain sub-declarations.
 *
 * @see #elements() for the elements of the declaration.
 */
public class ASTDeclaration extends ASTElement {

    private final ASTIdentifier keyword;
    private final List<@Nullable ASTElement> elements;

    public ASTDeclaration(@Nullable ASTIdentifier keyword, List<@Nullable ASTElement> elements) {
        super(ElementType.DECLARATION, CollectionUtil.mergeNonNull(elements, keyword));
        if (keyword == null) {
            this.value = elements.isEmpty() ? null : elements.get(0).value();
        } else {
            this.value = keyword.value();
        }
        this.keyword = keyword;
        this.elements = elements;
    }

    public ASTIdentifier keyword() {
        return keyword;
    }

    public List<@Nullable ASTElement> elements() {
        return elements;
    }

    public @Nullable ASTElement element(int index) {
        return elements.get(index);
    }

}
