package uk.ac.bham.codeclassroom.generator.jhipster;

import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedEntity;
import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedInheritanceModel;

/**
 * Generator that converts resolved inheritance models into JHipsterInheritance metadata.
 */
public class InheritanceMetadataGenerator {

    /**
     * Generates JHipsterInheritance metadata for a given entity name based on the resolved inheritance model.
     *
     * @param entityName               the name of the entity
     * @param resolvedInheritanceModel the resolved inheritance model from the resolver
     * @return the JHipsterInheritance metadata
     */
    public JHipsterInheritance generate(String entityName, ResolvedInheritanceModel resolvedInheritanceModel) {
        if (resolvedInheritanceModel == null) {
            throw new IllegalArgumentException("ResolvedInheritanceModel cannot be null");
        }

        ResolvedEntity resolved = resolvedInheritanceModel.findByName(entityName)
            .orElseThrow(() -> new IllegalArgumentException("Entity " + entityName + " not found in inheritance model"));

        return new JHipsterInheritance(
            resolved.parentName(),
            resolved.children(),
            resolved.depth(),
            resolved.generationOrder(),
            resolved.isRoot(),
            resolved.requiresInheritanceAnnotation()
        );
    }
}
