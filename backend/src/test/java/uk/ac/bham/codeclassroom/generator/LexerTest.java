package uk.ac.bham.codeclassroom.generator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import uk.ac.bham.codeclassroom.generator.exception.LexerException;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.token.Token;
import uk.ac.bham.codeclassroom.generator.token.TokenType;

class LexerTest {

    @Test
    void testKeywordsAndIdentifiers() {
        String source = "entity relationship extends to studentName";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        assertEquals(6, tokens.size()); // 5 tokens + EOF
        assertEquals(TokenType.ENTITY, tokens.get(0).type());
        assertEquals("entity", tokens.get(0).lexeme());

        assertEquals(TokenType.RELATIONSHIP, tokens.get(1).type());
        assertEquals("relationship", tokens.get(1).lexeme());

        assertEquals(TokenType.EXTENDS, tokens.get(2).type());
        assertEquals("extends", tokens.get(2).lexeme());

        assertEquals(TokenType.TO, tokens.get(3).type());
        assertEquals("to", tokens.get(3).lexeme());

        assertEquals(TokenType.IDENTIFIER, tokens.get(4).type());
        assertEquals("studentName", tokens.get(4).lexeme());

        assertEquals(TokenType.EOF, tokens.get(5).type());
    }

    @Test
    void testPrimitiveTypes() {
        String source = "String Integer Long Boolean BigDecimal LocalDate";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        assertEquals(7, tokens.size()); // 6 primitive types + EOF
        for (int i = 0; i < 6; i++) {
            assertEquals(TokenType.PRIMITIVE_TYPE, tokens.get(i).type());
        }
        assertEquals("String", tokens.get(0).lexeme());
        assertEquals("LocalDate", tokens.get(5).lexeme());
    }

    @Test
    void testGenericTypes() {
        String source = "List Set Map Collection";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        assertEquals(5, tokens.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(TokenType.GENERIC_TYPE, tokens.get(i).type());
        }
        assertEquals("List", tokens.get(0).lexeme());
    }

    @Test
    void testPunctuationAndBrackets() {
        String source = "{ } ( ) : , < >";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        assertEquals(9, tokens.size());
        assertEquals(TokenType.LBRACE, tokens.get(0).type());
        assertEquals(TokenType.RBRACE, tokens.get(1).type());
        assertEquals(TokenType.LPAREN, tokens.get(2).type());
        assertEquals(TokenType.RPAREN, tokens.get(3).type());
        assertEquals(TokenType.COLON, tokens.get(4).type());
        assertEquals(TokenType.COMMA, tokens.get(5).type());
        assertEquals(TokenType.LT, tokens.get(6).type());
        assertEquals(TokenType.GT, tokens.get(7).type());
    }

    @Test
    void testSingleLineComments() {
        String source = "entity // this is a single line comment\nStudent";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        assertEquals(3, tokens.size()); // entity, Student, EOF
        assertEquals(TokenType.ENTITY, tokens.get(0).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals("Student", tokens.get(1).lexeme());
    }

    @Test
    void testMultiLineComments() {
        String source = "entity /* comment starts\n continues\n ends */ Student";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        assertEquals(3, tokens.size()); // entity, Student, EOF
        assertEquals(TokenType.ENTITY, tokens.get(0).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals("Student", tokens.get(1).lexeme());
    }

    @Test
    void testUnterminatedMultiLineCommentThrows() {
        String source = "entity /* unclosed comment";
        Lexer lexer = new Lexer(source);
        
        LexerException ex = assertThrows(LexerException.class, lexer::tokenize);
        assertTrue(ex.getMessage().contains("Unterminated multi-line comment"));
        assertEquals(1, ex.getLine());
        assertEquals(8, ex.getColumn());
    }

    @Test
    void testInvalidCharacterThrows() {
        String source = "entity Student #";
        Lexer lexer = new Lexer(source);

        LexerException ex = assertThrows(LexerException.class, lexer::tokenize);
        assertTrue(ex.getMessage().contains("Unexpected character '#'"));
        assertEquals(1, ex.getLine());
        assertEquals(16, ex.getColumn());
    }

    @Test
    void testLineAndColumnTracking() {
        String source = "entity\n  Student {\n\tname String\n}";
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        // entity: line 1, col 1
        Token entityTok = tokens.get(0);
        assertEquals(1, entityTok.line());
        assertEquals(1, entityTok.column());

        // Student: line 2, col 3
        Token studentTok = tokens.get(1);
        assertEquals(2, studentTok.line());
        assertEquals(3, studentTok.column());

        // name: line 3, col 6 (\t treated as length 1)
        Token nameTok = tokens.get(3);
        assertEquals(3, nameTok.line());
        assertEquals(2, nameTok.column());
    }
}
