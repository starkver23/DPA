package uk.ac.bham.codeclassroom.generator.ast;

/**
 * Represents a single parameter within a method signature.
 * Example: Integer hours
 *
 * @param name the parameter name
 * @param type the parameter type reference
 */
public record ParameterNode(
    String name,
    TypeReference type
) {}
