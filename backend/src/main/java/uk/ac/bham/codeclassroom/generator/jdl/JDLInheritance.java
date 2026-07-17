package uk.ac.bham.codeclassroom.generator.jdl;

/**
 * Represents a single inheritance declaration in CodeClassroom Extended JDL.
 *
 * @param childEntity  the child entity name
 * @param parentEntity the parent entity name
 */
public record JDLInheritance(
    String childEntity,
    String parentEntity
) {}
