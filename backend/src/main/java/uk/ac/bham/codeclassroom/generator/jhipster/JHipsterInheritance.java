package uk.ac.bham.codeclassroom.generator.jhipster;

import java.util.List;
import java.util.Optional;

/**
 * Immutable metadata representing inheritance relationships and hierarchy for a JHipster entity.
 *
 * @param parentEntity                    the direct parent entity name (empty if root)
 * @param childEntities                   the direct child entity names
 * @param inheritanceDepth                depth in the inheritance hierarchy (root = 0, subclasses = parent depth + 1)
 * @param generationOrder                 topological generation order number
 * @param isRoot                          true if this entity is the root of the hierarchy
 * @param requiresInheritanceAnnotations  true if inheritance annotations will later be required
 */
public record JHipsterInheritance(
    Optional<String> parentEntity,
    List<String> childEntities,
    int inheritanceDepth,
    int generationOrder,
    boolean isRoot,
    boolean requiresInheritanceAnnotations
) {}
