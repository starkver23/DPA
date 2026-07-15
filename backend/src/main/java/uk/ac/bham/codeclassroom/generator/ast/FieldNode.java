package uk.ac.bham.codeclassroom.generator.ast;

/**
 * Represents a field declaration in an entity block.
 * Example: email String
 *
 * @param name the name of the field
 * @param type the type reference of the field
 */
public record FieldNode(
    String name,
    TypeReference type
) {}
