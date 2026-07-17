package uk.ac.bham.codeclassroom.generator.jdl;

import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;

import java.util.List;

/**
 * Represents the complete Extended JDL document model.
 *
 * @param entities               the list of entities
 * @param relationships          the list of relationships
 * @param inheritanceDeclarations the list of inheritance declarations
 * @param configuration          the configuration metadata
 */
public record ExtendedJDLDocument(
    List<JDLEntity> entities,
    List<JDLRelationship> relationships,
    List<JDLInheritance> inheritanceDeclarations,
    JHipsterProjectConfiguration configuration
) {
    /**
     * Helper for Mustache serialization.
     * Returns true if there are custom inheritance declarations.
     *
     * @return true if inheritances exist, false otherwise
     */
    public boolean hasInheritances() {
        return inheritanceDeclarations != null && !inheritanceDeclarations.isEmpty();
    }

    /**
     * Helper for Mustache serialization.
     * Returns true if there are entities in the document.
     *
     * @return true if entities exist, false otherwise
     */
    public boolean hasEntities() {
        return entities != null && !entities.isEmpty();
    }

    /**
     * Helper for Mustache serialization.
     * Returns a distinct, alphabetically sorted, comma-separated list of entity names.
     *
     * @return the formatted entity list, or empty string if none exist
     */
    public String formattedEntitiesList() {
        if (entities == null || entities.isEmpty()) {
            return "";
        }
        java.util.List<String> names = entities.stream()
            .map(JDLEntity::name)
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
        return String.join(", ", names);
    }
}
