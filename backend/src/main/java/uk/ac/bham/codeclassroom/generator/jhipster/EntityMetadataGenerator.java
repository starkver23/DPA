package uk.ac.bham.codeclassroom.generator.jhipster;

import uk.ac.bham.codeclassroom.generator.ast.EntityNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generator that converts CDL EntityNodes into JHipsterEntity metadata.
 */
public class EntityMetadataGenerator {

    /**
     * Converts an EntityNode into JHipsterEntity metadata.
     *
     * @param entityNode    the CDL EntityNode
     * @param inheritance   the pre-calculated JHipsterInheritance metadata for this entity
     * @param relationships all JHipsterRelationships where this entity is the source
     * @return the JHipsterEntity metadata
     */
    public JHipsterEntity generate(
        EntityNode entityNode,
        JHipsterInheritance inheritance,
        List<JHipsterRelationship> relationships
    ) {
        if (entityNode == null) {
            throw new IllegalArgumentException("EntityNode cannot be null");
        }
        if (inheritance == null) {
            throw new IllegalArgumentException("JHipsterInheritance cannot be null");
        }
        if (relationships == null) {
            throw new IllegalArgumentException("Relationships list cannot be null");
        }

        // Map fields
        List<JHipsterField> fields = entityNode.fields().stream()
            .map(f -> new JHipsterField(f.name(), f.type()))
            .collect(Collectors.toList());

        // Map methods
        List<JHipsterMethod> methods = entityNode.methods().stream()
            .map(m -> new JHipsterMethod(
                m.name(),
                m.parameters().stream()
                    .map(p -> new JHipsterParameter(p.name(), p.type()))
                    .collect(Collectors.toList()),
                m.returnType()
            ))
            .collect(Collectors.toList());

        return new JHipsterEntity(
            entityNode.name(),
            List.copyOf(fields),
            List.copyOf(methods),
            List.copyOf(relationships),
            inheritance
        );
    }
}
