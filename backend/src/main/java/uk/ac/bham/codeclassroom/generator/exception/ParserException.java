package uk.ac.bham.codeclassroom.generator.exception;

import uk.ac.bham.codeclassroom.generator.token.Token;

/**
 * Exception thrown when the Parser encounters a syntax or grammar violation.
 */
public class ParserException extends RuntimeException {
    private final Token token;
    private final int line;
    private final int column;

    public ParserException(String message, Token token) {
        super(String.format("Syntax Error [line %d, column %d] at '%s': %s",
                token != null ? token.line() : 0,
                token != null ? token.column() : 0,
                token != null ? token.lexeme() : "EOF",
                message));
        this.token = token;
        this.line = token != null ? token.line() : 0;
        this.column = token != null ? token.column() : 0;
    }

    public Token getToken() {
        return token;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
