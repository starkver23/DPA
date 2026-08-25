package uk.ac.bham.codeclassroom.generator.jdl;

import java.util.List;

/**
 * Represents an entity declaration in CodeClassroom JDL.
 *
 * @param name          the name of the entity
 * @param fields        the fields declared inside the entity
 * @param relationships the relationships associated with this entity
 */
public record JDLEntity(
    String name,
    boolean abstractClass,
    List<JDLField> fields,
    List<JDLRelationship> relationships
) {}
