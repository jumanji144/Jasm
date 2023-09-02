package me.darknet.assembler.ast;

import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
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
    public ASTElement(ElementType type, List<? extends ASTElement> children) {
        for (ASTElement child : children) {
            if (child != null) {
                child.parent = this;
            }
        }
        this.type = type;
        this.children = (List<ASTElement>) children;
    }

    @Contract(pure = true)
    public String content() {
        return value == null ? null : value.getContent();
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

    public List<ASTElement> children() {
        return children;
    }

    public Location location() {
        if (value == null) {
            // go through children
            for (ASTElement child : children) {
                if(child == null) {
                    continue;
                }
                Location location = child.location();
                if (location != null) {
                    return location;
                }
            }
        } else {
            return value.getLocation();
        }
        return null;
    }
}
