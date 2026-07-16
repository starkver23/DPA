package uk.ac.bham.codeclassroom.generator.parser;

import uk.ac.bham.codeclassroom.generator.ast.*;
import uk.ac.bham.codeclassroom.generator.exception.ParserException;
import uk.ac.bham.codeclassroom.generator.token.Token;
import uk.ac.bham.codeclassroom.generator.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A recursive-descent parser that converts a stream of CDL tokens into an immutable AST.
 */
public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens != null ? tokens : List.of();
    }

    /**
     * Parses the token stream and returns the root AST node (CompilationUnit).
     *
     * @return the immutable AST CompilationUnit node
     * @throws ParserException if a syntax error is detected
     */
    public CompilationUnit parse() {
        List<EntityNode> entities = new ArrayList<>();
        List<RelationshipNode> relationships = new ArrayList<>();

        while (!isAtEnd()) {
            if (match(TokenType.ENTITY)) {
                entities.add(parseEntity());
            } else if (match(TokenType.RELATIONSHIP)) {
                relationships.addAll(parseRelationshipBlock());
            } else {
                throw new ParserException("Unexpected top-level token: " + peek().lexeme(), peek());
            }
        }

        return new CompilationUnit(List.copyOf(entities), List.copyOf(relationships));
    }

    private EntityNode parseEntity() {
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected entity name identifier.");
        String entityName = nameToken.lexeme();

        Optional<InheritanceNode> inheritance = Optional.empty();
        if (match(TokenType.EXTENDS)) {
            Token parentToken = consume(TokenType.IDENTIFIER, "Expected parent entity name after 'extends'.");
            inheritance = Optional.of(new InheritanceNode(parentToken.lexeme()));
        }

        consume(TokenType.LBRACE, "Expected '{' to open entity body.");

        List<FieldNode> fields = new ArrayList<>();
        List<MethodNode> methods = new ArrayList<>();

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            parseEntityMember(fields, methods);
        }

        consume(TokenType.RBRACE, "Expected '}' to close entity body.");

        return new EntityNode(entityName, inheritance, List.copyOf(fields), List.copyOf(methods));
    }

    private void parseEntityMember(List<FieldNode> fields, List<MethodNode> methods) {
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected field or method name identifier.");
        String memberName = nameToken.lexeme();

        // LL(1) Lookahead: If next token is '(' it's a method declaration, otherwise it's a field declaration
        if (check(TokenType.LPAREN)) {
            methods.add(parseMethod(memberName));
        } else {
            fields.add(parseField(memberName));
        }
    }

    private FieldNode parseField(String name) {
        TypeReference type = parseTypeReference();
        return new FieldNode(name, type);
    }

    private MethodNode parseMethod(String name) {
        consume(TokenType.LPAREN, "Expected '(' after method name.");

        List<ParameterNode> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                TypeReference paramType = parseTypeReference();
                Token paramNameToken = consume(TokenType.IDENTIFIER, "Expected parameter name identifier.");
                parameters.add(new ParameterNode(paramNameToken.lexeme(), paramType));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "Expected ')' to close method parameter list.");

        Optional<TypeReference> returnType = Optional.empty();
        if (match(TokenType.COLON)) {
            returnType = Optional.of(parseTypeReference());
        }

        return new MethodNode(name, List.copyOf(parameters), returnType);
    }

    private TypeReference parseTypeReference() {
        Token baseToken;
        if (match(TokenType.PRIMITIVE_TYPE, TokenType.GENERIC_TYPE, TokenType.IDENTIFIER)) {
            baseToken = previous();
        } else {
            throw new ParserException("Expected field or parameter type reference.", peek());
        }

        String baseType = baseToken.lexeme();
        Optional<TypeReference> genericType = Optional.empty();

        if (match(TokenType.LT)) {
            TypeReference innerType = parseTypeReference();
            genericType = Optional.of(innerType);
            consume(TokenType.GT, "Expected '>' to close generic type argument.");
        }

        return new TypeReference(baseType, genericType);
    }

    private List<RelationshipNode> parseRelationshipBlock() {
        Token typeToken = consume(TokenType.IDENTIFIER, "Expected relationship type (OneToOne, OneToMany, ManyToMany).");
        RelationshipType relType;
        try {
            relType = RelationshipType.valueOf(typeToken.lexeme());
        } catch (IllegalArgumentException e) {
            throw new ParserException("Unsupported relationship type: " + typeToken.lexeme(), typeToken);
        }

        consume(TokenType.LBRACE, "Expected '{' to open relationship block.");

        List<RelationshipNode> rules = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            rules.add(parseRelationshipRule(relType));
        }

        consume(TokenType.RBRACE, "Expected '}' to close relationship block.");
        return rules;
    }

    private RelationshipNode parseRelationshipRule(RelationshipType relType) {
        Token sourceToken = consume(TokenType.IDENTIFIER, "Expected source entity name in relationship rule.");
        String sourceEntity = sourceToken.lexeme();

        Optional<String> sourceProperty = parseOptionalProperty();

        consume(TokenType.TO, "Expected keyword 'to' between source and target entities.");

        Token targetToken = consume(TokenType.IDENTIFIER, "Expected target entity name in relationship rule.");
        String targetEntity = targetToken.lexeme();

        Optional<String> targetProperty = parseOptionalProperty();

        return new RelationshipNode(relType, sourceEntity, sourceProperty, targetEntity, targetProperty);
    }

    private Optional<String> parseOptionalProperty() {
        if (match(TokenType.LBRACE)) {
            Token propToken = consume(TokenType.IDENTIFIER, "Expected property name identifier inside braces.");
            consume(TokenType.RBRACE, "Expected '}' after property name.");
            return Optional.of(propToken.lexeme());
        }
        return Optional.empty();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String errorMessage) {
        if (check(type)) return advance();
        throw new ParserException(errorMessage, peek());
    }
}
