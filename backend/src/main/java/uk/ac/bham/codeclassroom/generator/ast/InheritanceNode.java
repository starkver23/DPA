package uk.ac.bham.codeclassroom.generator.ast;

/**
 * Represents the inheritance definition of an entity.
 * Example: entity Student extends Person { }
 *
 * @param parentName the name of the parent entity being extended
 */
public record InheritanceNode(
    String parentName
) {}
