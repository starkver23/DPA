package uk.ac.bham.codeclassroom.generator.jdl;

import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterEntity;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterField;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProject;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterRelationship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generator that converts a JHipsterProject metadata object into an ExtendedJDLDocument.
 * Performs all sorting and mapping logic to ensure deterministic and standard JDL compliance.
 */
public class ExtendedJDLGenerator {

    /**
     * Converts a JHipsterProject into an ExtendedJDLDocument.
     * All entities, fields, relationships, and inheritance declarations are sorted alphabetically.
     *
     * @param project the source JHipster intermediate metadata project
     * @return the generated ExtendedJDLDocument representation
     */
    public ExtendedJDLDocument generate(JHipsterProject project) {
        if (project == null) {
            throw new IllegalArgumentException("JHipsterProject cannot be null");
        }

        // 1. Map relationships globally
        List<JDLRelationship> rawRelationships = project.relationships().stream()
            .map(this::mapRelationship)
            .collect(Collectors.toList());

        // Deduplicate
        List<JDLRelationship> jdlRelationships = deduplicateRelationships(rawRelationships);

        // Sort alphabetically to ensure deterministic formatting
        jdlRelationships.sort(Comparator.comparing((JDLRelationship r) -> r.type().name())
            .thenComparing(JDLRelationship::sourceEntity)
            .thenComparing(JDLRelationship::targetEntity));

        // 2. Map entities and collect inheritance declarations
        List<JDLEntity> jdlEntities = new ArrayList<>();
        List<JDLInheritance> jdlInheritances = new ArrayList<>();

        for (JHipsterEntity entity : project.entities()) {
            // Sort fields alphabetically inside entity
            List<JDLField> jdlFields = entity.fields().stream()
                .map(this::mapField)
                .sorted(Comparator.comparing(JDLField::name))
                .collect(Collectors.toList());

            // Filter relationships where this entity is the source (keeping sorted order)
            List<JDLRelationship> entityRels = jdlRelationships.stream()
                .filter(r -> r.sourceEntity().equals(entity.entityName()))
                .collect(Collectors.toList());

            JDLEntity jdlEntity = new JDLEntity(entity.entityName(), entity.abstractClass(), jdlFields, entityRels);
            jdlEntities.add(jdlEntity);

            // Extract inheritance if parent entity is present
            if (entity.inheritance() != null && entity.inheritance().parentEntity().isPresent()) {
                jdlInheritances.add(new JDLInheritance(
                    entity.entityName(),
                    entity.inheritance().parentEntity().get()
                ));
            }
        }

        // Sort entities alphabetically
        List<JDLEntity> sortedEntities = jdlEntities.stream()
            .sorted(Comparator.comparing(JDLEntity::name))
            .collect(Collectors.toList());

        // Sort inheritance declarations alphabetically by child name
        List<JDLInheritance> sortedInheritances = jdlInheritances.stream()
            .sorted(Comparator.comparing(JDLInheritance::childEntity)
                .thenComparing(JDLInheritance::parentEntity))
            .collect(Collectors.toList());

        return new ExtendedJDLDocument(
            List.copyOf(sortedEntities),
            List.copyOf(jdlRelationships),
            List.copyOf(sortedInheritances),
            project.configuration(),
            project.interfaces(),
            project.originalEntities()
        );
    }

    private JDLField mapField(JHipsterField field) {
        return new JDLField(field.name(), field.type().toString());
    }

    private JDLRelationship mapRelationship(JHipsterRelationship relationship) {
        JDLRelationshipType type;
        switch (relationship.type()) {
            case OneToOne -> type = JDLRelationshipType.OneToOne;
            case OneToMany -> type = JDLRelationshipType.OneToMany;
            case ManyToOne -> type = JDLRelationshipType.ManyToOne;
            case ManyToMany -> type = JDLRelationshipType.ManyToMany;
            default -> throw new IllegalArgumentException("Unsupported relationship type: " + relationship.type());
        }

        return new JDLRelationship(
            type,
            relationship.sourceEntity(),
            relationship.sourceProperty(),
            relationship.targetEntity(),
            relationship.targetProperty()
        );
    }

    private List<JDLRelationship> deduplicateRelationships(List<JDLRelationship> relationships) {
        // ponytail: deduplicate bidirectional/inverse representations with a simple O(N^2) list scan. Highly performant for normal model schema sizes.
        List<JDLRelationship> unique = new ArrayList<>();
        for (JDLRelationship r : relationships) {
            boolean isDup = false;
            for (JDLRelationship existing : unique) {
                if (isSameOrReversed(r, existing)) {
                    isDup = true;
                    break;
                }
            }
            if (!isDup) {
                unique.add(r);
            }
        }
        return unique;
    }

    private boolean isSameOrReversed(JDLRelationship r1, JDLRelationship r2) {
        // Exact match
        if (r1.type() == r2.type() &&
            r1.sourceEntity().equals(r2.sourceEntity()) &&
            r1.targetEntity().equals(r2.targetEntity()) &&
            r1.sourceProperty().equals(r2.sourceProperty()) &&
            r1.targetProperty().equals(r2.targetProperty())) {
            return true;
        }

        // Reversed match
        boolean isReversedEntitiesAndProps = 
            r1.sourceEntity().equals(r2.targetEntity()) &&
            r1.targetEntity().equals(r2.sourceEntity()) &&
            r1.sourceProperty().equals(r2.targetProperty()) &&
            r1.targetProperty().equals(r2.sourceProperty());

        if (isReversedEntitiesAndProps) {
            if (r1.type() == JDLRelationshipType.OneToOne && r2.type() == JDLRelationshipType.OneToOne) {
                return true;
            }
            if (r1.type() == JDLRelationshipType.ManyToMany && r2.type() == JDLRelationshipType.ManyToMany) {
                return true;
            }
            if ((r1.type() == JDLRelationshipType.OneToMany && r2.type() == JDLRelationshipType.ManyToOne) ||
                (r1.type() == JDLRelationshipType.ManyToOne && r2.type() == JDLRelationshipType.OneToMany)) {
                return true;
            }
        }

        return false;
    }
}
