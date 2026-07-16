package uk.ac.bham.codeclassroom.generator.token;

/**
 * Immutable representation of a Lexical Token in the CDL compilation pipeline.
 *
 * @param type    the classification of the token
 * @param lexeme  the exact substring of the source code matched
 * @param line    the 1-based line number where the token starts
 * @param column  the 1-based column number where the token starts
 */
public record Token(
    TokenType type,
    String lexeme,
    int line,
    int column
) {
    @Override
    public String toString() {
        return String.format("Token[%s, '%s', line=%d, col=%d]", type, lexeme, line, column);
    }
}
