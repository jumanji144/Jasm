package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;

public class ASTException extends ASTElement {

    private final ASTIdentifier start;
    private final ASTIdentifier end;
    private final ASTIdentifier handler;
    private final ASTIdentifier type;

    public ASTException(ASTIdentifier start, ASTIdentifier end, ASTIdentifier handler, ASTIdentifier type) {
        super(ElementType.EXCEPTION, start, end, handler, type);
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
    }

    public ASTIdentifier start() {
        return start;
    }

    public ASTIdentifier end() {
        return end;
    }

    public ASTIdentifier handler() {
        return handler;
    }

    public ASTIdentifier exceptionType() {
        return type;
    }

}
