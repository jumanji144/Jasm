package me.darknet.assembler.parser;

import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.Token;

import java.util.List;

/**
 * {@link Result} with a list of {@link Token}s.
 *
 * @param <T>
 *            The type of the value.
 */
public class ParsingResult<T> extends Result<T> {

    private final List<Token> comments;

    public ParsingResult(T value, List<Error> errors, List<Token> comments) {
        super(value, errors, List.of());
        this.comments = comments;
    }

    public List<Token> comments() {
        return comments;
    }

}
