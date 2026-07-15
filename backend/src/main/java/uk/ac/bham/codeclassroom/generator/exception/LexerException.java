package uk.ac.bham.codeclassroom.generator.exception;

/**
 * Exception thrown when the Lexer encounters an unrecognized character or malformed token.
 */
public class LexerException extends RuntimeException {
    private final int line;
    private final int column;
    private final String lexeme;

    public LexerException(String message, int line, int column, String lexeme) {
        super(String.format("Lexical Error [line %d, column %d] near '%s': %s", line, column, lexeme, message));
        this.line = line;
        this.column = column;
        this.lexeme = lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getLexeme() {
        return lexeme;
    }
}
