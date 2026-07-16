package uk.ac.bham.codeclassroom.generator.ast;

import java.util.Optional;

/**
 * Represents a single relationship connection rule between two entities.
 * Example: Student{courses} to Course{students} inside a relationship ManyToMany block.
 *
 * @param type            the type of relationship (OneToOne, OneToMany, ManyToMany)
 * @param sourceEntity    the name of the source entity
 * @param sourceProperty  the optional property name mapped in braces on the source entity
 * @param targetEntity    the name of the target entity
 * @param targetProperty  the optional property name mapped in braces on the target entity
 */
public record RelationshipNode(
    RelationshipType type,
    String sourceEntity,
    Optional<String> sourceProperty,
    String targetEntity,
    Optional<String> targetProperty
) {}
