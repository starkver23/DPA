package uk.ac.bham.codeclassroom.generator.jhipster;

import java.util.List;

/**
 * Immutable metadata representing a JHipster entity.
 *
 * @param entityName    the name of the entity
 * @param fields        the fields defined on the entity
 * @param methods       the methods defined on the entity
 * @param relationships the list of relationships where this entity is the source
 * @param inheritance   the inheritance hierarchy metadata for this entity
 */
public record JHipsterEntity(
    String entityName,
    List<JHipsterField> fields,
    List<JHipsterMethod> methods,
    List<JHipsterRelationship> relationships,
    JHipsterInheritance inheritance
) {}
