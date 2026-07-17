package uk.ac.bham.codeclassroom.generator.jhipster;

import java.util.Optional;

/**
 * Immutable metadata representing a relationship between entities in a JHipster project.
 *
 * @param type           the JHipster-compatible relationship type
 * @param sourceEntity   the name of the source entity
 * @param sourceProperty the optional property/navigation name on the source entity
 * @param targetEntity   the name of the target entity
 * @param targetProperty the optional property/navigation name on the target entity
 */
public record JHipsterRelationship(
    JHipsterRelationshipType type,
    String sourceEntity,
    Optional<String> sourceProperty,
    String targetEntity,
    Optional<String> targetProperty
) {}
