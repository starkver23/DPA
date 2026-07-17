package uk.ac.bham.codeclassroom.generator.jhipster.template;

import uk.ac.bham.codeclassroom.generator.jhipster.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder that converts JHipster intermediate metadata into TemplateGenerationContext.
 */
public class TemplateModelBuilder {

    /**
     * Builds the template generation context for a JHipster entity in the project.
     *
     * @param entity  the entity being generated
     * @param project the JHipsterProject containing all metadata
     * @return the template generation context
     */
    public TemplateGenerationContext build(JHipsterEntity entity, JHipsterProject project) {
        if (entity == null || project == null) {
            throw new IllegalArgumentException("Entity and project cannot be null");
        }

        String basePackage = project.configuration().basePackage();

        // 1. Gather all inherited fields by traversing up the inheritance tree
        List<JHipsterField> inheritedFields = new ArrayList<>();
        String parentName = entity.inheritance().parentEntity().orElse(null);
        while (parentName != null) {
            final String nameToFind = parentName;
            JHipsterEntity parentEntity = project.entities().stream()
                .filter(e -> e.entityName().equals(nameToFind))
                .findFirst()
                .orElse(null);

            if (parentEntity != null) {
                inheritedFields.addAll(parentEntity.fields());
                parentName = parentEntity.inheritance().parentEntity().orElse(null);
            } else {
                parentName = null;
            }
        }

        // 2. Build local field maps
        List<Map<String, String>> localFields = entity.fields().stream()
            .map(this::mapFieldToMap)
            .collect(Collectors.toList());

        // 3. Build inherited field maps
        List<Map<String, String>> mappedInheritedFields = inheritedFields.stream()
            .map(this::mapFieldToMap)
            .collect(Collectors.toList());

        // 4. Build relationships mapped to JHipster-compliant structures
        List<Map<String, Object>> relationshipMaps = entity.relationships().stream()
            .map(rel -> {
                Map<String, Object> r = new HashMap<>();
                r.put("relationshipType", rel.type().toString());
                r.put("otherEntityName", rel.targetEntity());
                r.put("otherEntityNameLower", rel.targetEntity().substring(0, 1).toLowerCase() + rel.targetEntity().substring(1));
                r.put("relationshipName", rel.sourceProperty().orElse(rel.targetEntity().substring(0, 1).toLowerCase() + rel.targetEntity().substring(1)));
                r.put("otherEntityRelationshipName", rel.targetProperty().orElse(""));
                return r;
            })
            .collect(Collectors.toList());

        // 5. Build full context
        return new TemplateGenerationContext(
            entity.entityName(),
            entity.entityName().substring(0, 1).toLowerCase() + entity.entityName().substring(1),
            entity.entityName().toLowerCase() + "s",
            entity.inheritance().parentEntity().orElse(""),
            entity.inheritance().isRoot(),
            entity.inheritance().isRoot() && entity.inheritance().requiresInheritanceAnnotations(),
            entity.inheritance().parentEntity().isPresent(), // requiresExtends
            basePackage,
            localFields,
            mappedInheritedFields,
            relationshipMaps,
            project.configuration()
        );
    }

    private Map<String, String> mapFieldToMap(JHipsterField field) {
        Map<String, String> map = new HashMap<>();
        map.put("name", field.name());
        map.put("type", field.type().toString());
        String cap = field.name().substring(0, 1).toUpperCase() + field.name().substring(1);
        map.put("capitalizedName", cap);
        return map;
    }
}
