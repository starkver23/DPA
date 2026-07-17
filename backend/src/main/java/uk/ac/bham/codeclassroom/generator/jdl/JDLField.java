package uk.ac.bham.codeclassroom.generator.jdl;

/**
 * Represents a field declaration inside an entity in CodeClassroom JDL.
 *
 * @param name the field name
 * @param type the field type representation
 */
public record JDLField(
    String name,
    String type
) {}
