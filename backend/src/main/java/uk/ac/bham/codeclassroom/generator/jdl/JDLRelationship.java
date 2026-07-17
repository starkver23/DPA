package uk.ac.bham.codeclassroom.generator.jdl;

import java.util.Optional;

/**
 * Represents a relationship definition in JDL.
 *
 * @param type           the JDL relationship type
 * @param sourceEntity   the name of the source entity
 * @param sourceProperty the optional property name on the source entity (navigation property)
 * @param targetEntity   the name of the target entity
 * @param targetProperty the optional property name on the target entity (navigation property)
 */
public record JDLRelationship(
    JDLRelationshipType type,
    String sourceEntity,
    Optional<String> sourceProperty,
    String targetEntity,
    Optional<String> targetProperty
) {
    /**
     * Helper for Mustache serialization.
     * Returns the formatted source property with braces, or empty string if not present.
     *
     * @return formatted source property
     */
    public String formattedSourceProperty() {
        return sourceProperty.map(prop -> "{" + prop + "}").orElse("");
    }

    /**
     * Helper for Mustache serialization.
     * Returns the formatted target property with braces, or empty string if not present.
     *
     * @return formatted target property
     */
    public String formattedTargetProperty() {
        return targetProperty.map(prop -> "{" + prop + "}").orElse("");
    }
}
