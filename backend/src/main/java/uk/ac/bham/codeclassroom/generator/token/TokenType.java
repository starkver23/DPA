package uk.ac.bham.codeclassroom.generator.token;

/**
 * Enumeration of all token types recognized by the CDL Lexer.
 */
public enum TokenType {
    // Keywords
    ENTITY,
    RELATIONSHIP,
    EXTENDS,
    TO,

    // Types
    PRIMITIVE_TYPE,  // String, Integer, Long, Boolean, BigDecimal, LocalDate
    GENERIC_TYPE,     // List, Set, Map, Collection

    // Identifiers
    IDENTIFIER,

    // Punctuation and brackets
    LBRACE,     // {
    RBRACE,     // }
    LPAREN,     // (
    RPAREN,     // )
    COLON,      // :
    COMMA,      // ,
    LT,         // <
    GT,         // >

    // End of File
    EOF
}
