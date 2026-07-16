package uk.ac.bham.codeclassroom.generator.lexer;

import uk.ac.bham.codeclassroom.generator.exception.LexerException;
import uk.ac.bham.codeclassroom.generator.token.Token;
import uk.ac.bham.codeclassroom.generator.token.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs lexical analysis (tokenization) on CDL (CodeClassroom Domain Language) source text.
 */
public class Lexer {
    private final String source;
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;

    private static final Map<String, TokenType> KEYWORDS;

    static {
        KEYWORDS = new HashMap<>();
        KEYWORDS.put("entity", TokenType.ENTITY);
        KEYWORDS.put("relationship", TokenType.RELATIONSHIP);
        KEYWORDS.put("extends", TokenType.EXTENDS);
        KEYWORDS.put("to", TokenType.TO);

        // Primitive Types
        KEYWORDS.put("String", TokenType.PRIMITIVE_TYPE);
        KEYWORDS.put("Integer", TokenType.PRIMITIVE_TYPE);
        KEYWORDS.put("Long", TokenType.PRIMITIVE_TYPE);
        KEYWORDS.put("Boolean", TokenType.PRIMITIVE_TYPE);
        KEYWORDS.put("BigDecimal", TokenType.PRIMITIVE_TYPE);
        KEYWORDS.put("LocalDate", TokenType.PRIMITIVE_TYPE);

        // Generic Types
        KEYWORDS.put("List", TokenType.GENERIC_TYPE);
        KEYWORDS.put("Set", TokenType.GENERIC_TYPE);
        KEYWORDS.put("Map", TokenType.GENERIC_TYPE);
        KEYWORDS.put("Collection", TokenType.GENERIC_TYPE);
    }

    public Lexer(String source) {
        this.source = source != null ? source : "";
    }

    /**
     * Scans the entire source and produces a list of Tokens, ending with EOF.
     *
     * @return list of parsed Token objects
     * @throws LexerException if an invalid token or unterminated comment is found
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            start = current;
            scanToken(tokens);
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private void scanToken(List<Token> tokens) {
        char c = advance();
        switch (c) {
            case '{': addToken(tokens, TokenType.LBRACE); break;
            case '}': addToken(tokens, TokenType.RBRACE); break;
            case '(': addToken(tokens, TokenType.LPAREN); break;
            case ')': addToken(tokens, TokenType.RPAREN); break;
            case ':': addToken(tokens, TokenType.COLON); break;
            case ',': addToken(tokens, TokenType.COMMA); break;
            case '<': addToken(tokens, TokenType.LT); break;
            case '>': addToken(tokens, TokenType.GT); break;
            case '/':
                if (peek() == '/') {
                    // Single-line comment: skip until end of line
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (peek() == '*') {
                    // Multi-line comment: skip until closing token */
                    advance(); // Consume '*'
                    int commentStartLine = line;
                    int commentStartCol = column - 2;
                    boolean closed = false;
                    while (!isAtEnd()) {
                        if (peek() == '*' && peekNext() == '/') {
                            advance(); // Consume '*'
                            advance(); // Consume '/'
                            closed = true;
                            break;
                        }
                        advance();
                    }
                    if (!closed) {
                        throw new LexerException("Unterminated multi-line comment", commentStartLine, commentStartCol, "/*");
                    }
                } else {
                    throw new LexerException("Unexpected character '/'", line, column - 1, "/");
                }
                break;
            case ' ':
            case '\r':
            case '\t':
            case '\n':
                // Ignore whitespace characters
                break;
            default:
                if (isAlpha(c)) {
                    identifier(tokens);
                } else {
                    throw new LexerException("Unexpected character '" + c + "'", line, column - 1, String.valueOf(c));
                }
                break;
        }
    }

    private void identifier(List<Token> tokens) {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.get(text);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        int tokenColumn = column - (current - start);
        tokens.add(new Token(type, text, line, tokenColumn));
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || (c >= '0' && c <= '9');
    }

    private void addToken(List<Token> tokens, TokenType type) {
        String text = source.substring(start, current);
        int tokenColumn = column - (current - start);
        tokens.add(new Token(type, text, line, tokenColumn));
    }
}
