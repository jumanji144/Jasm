package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import org.jetbrains.annotations.NotNull;

public class ASTOuterMethod extends ASTElement {
    private final ASTIdentifier methodName;
    private final ASTIdentifier methodDesc;

    public ASTOuterMethod(@NotNull ASTIdentifier methodName, @NotNull ASTIdentifier methodDesc) {
        super(ElementType.OUTER_METHOD, methodName, methodDesc);
        this.methodName =methodName;
        this.methodDesc=methodDesc;
    }

    @NotNull
    public ASTIdentifier getMethodName() {
        return methodName;
    }

    @NotNull
    public ASTIdentifier getMethodDesc() {
        return methodDesc;
    }
}
