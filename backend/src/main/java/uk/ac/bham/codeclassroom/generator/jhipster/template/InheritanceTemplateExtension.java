package uk.ac.bham.codeclassroom.generator.jhipster.template;

import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterInheritance;

/**
 * Helper class that provides JHipster-specific inheritance extension logic for template rendering.
 */
public class InheritanceTemplateExtension {

    /**
     * Determines the JPA inheritance annotation for a JHipster inheritance metadata structure.
     * Only the root entity should receive the @Inheritance(strategy = InheritanceType.JOINED) annotation.
     *
     * @param inheritance the JHipster inheritance metadata
     * @return the JPA inheritance annotation string, or empty if not appropriate
     */
    public String getInheritanceAnnotation(JHipsterInheritance inheritance) {
        if (inheritance == null) {
            return "";
        }
        if (inheritance.isRoot() && inheritance.requiresInheritanceAnnotations()) {
            return "@Inheritance(strategy = InheritanceType.JOINED)";
        }
        return "";
    }

    /**
     * Formats the Java "extends" clause for an entity based on its parent.
     *
     * @param inheritance the JHipster inheritance metadata
     * @return the formatted Java "extends" clause (e.g., " extends Person"), or empty if none
     */
    public String getExtendsClause(JHipsterInheritance inheritance) {
        if (inheritance == null) {
            return "";
        }
        return inheritance.parentEntity()
            .map(parent -> " extends " + parent)
            .orElse("");
    }

    /**
     * Formats the Java "extends" clause for a DTO class based on its parent.
     *
     * @param inheritance the JHipster inheritance metadata
     * @return the formatted Java "extends" clause for the DTO (e.g., " extends PersonDTO"), or empty if none
     */
    public String getDtoExtendsClause(JHipsterInheritance inheritance) {
        if (inheritance == null) {
            return "";
        }
        return inheritance.parentEntity()
            .map(parent -> " extends " + parent + "DTO")
            .orElse("");
    }
}
