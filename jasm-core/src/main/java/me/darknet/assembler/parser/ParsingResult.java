package me.darknet.assembler.parser;

import me.darknet.assembler.ast.primitive.ASTComment;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;

import java.util.List;

/**
 * {@link Result} with a list of {@link ASTComment}s.
 *
 * @param <T>
 *            The type of the value.
 */
public class ParsingResult<T> extends Result<T> {

    private final List<ASTComment> comments;

    public ParsingResult(T value, List<Error> errors, List<ASTComment> comments) {
        super(value, errors);
        this.comments = comments;
    }

    public List<ASTComment> comments() {
        return comments;
    }

}
