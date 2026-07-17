package uk.ac.bham.codeclassroom.generator.jhipster;

import uk.ac.bham.codeclassroom.generator.ast.RelationshipNode;
import uk.ac.bham.codeclassroom.generator.ast.RelationshipType;

import java.util.ArrayList;
import java.util.List;

/**
 * Generator that converts CDL relationship nodes into JHipsterRelationship metadata.
 * Supports OneToOne, OneToMany, ManyToOne, and ManyToMany, preserving navigation properties.
 */
public class RelationshipMetadataGenerator {

    /**
     * Converts a list of CDL relationship nodes into JHipsterRelationship metadata.
     * Generates both sides of the relationships to ensure proper JHipster-compatible bidirectional mapping.
     *
     * @param nodes the CDL relationship nodes
     * @return a list of JHipster relationships
     */
    public List<JHipsterRelationship> generate(List<RelationshipNode> nodes) {
        if (nodes == null) {
            return List.of();
        }

        List<JHipsterRelationship> relationships = new ArrayList<>();

        for (RelationshipNode node : nodes) {
            if (node.type() == RelationshipType.OneToOne) {
                // Source side: OneToOne
                relationships.add(new JHipsterRelationship(
                    JHipsterRelationshipType.OneToOne,
                    node.sourceEntity(),
                    node.sourceProperty(),
                    node.targetEntity(),
                    node.targetProperty()
                ));

                // Target side: OneToOne (bidirectional if targetProperty is present)
                if (node.targetProperty().isPresent()) {
                    relationships.add(new JHipsterRelationship(
                        JHipsterRelationshipType.OneToOne,
                        node.targetEntity(),
                        node.targetProperty(),
                        node.sourceEntity(),
                        node.sourceProperty()
                    ));
                }
            } else if (node.type() == RelationshipType.OneToMany) {
                // Source side: OneToMany
                relationships.add(new JHipsterRelationship(
                    JHipsterRelationshipType.OneToMany,
                    node.sourceEntity(),
                    node.sourceProperty(),
                    node.targetEntity(),
                    node.targetProperty()
                ));

                // Target side: ManyToOne (inverse of OneToMany)
                relationships.add(new JHipsterRelationship(
                    JHipsterRelationshipType.ManyToOne,
                    node.targetEntity(),
                    node.targetProperty(),
                    node.sourceEntity(),
                    node.sourceProperty()
                ));
            } else if (node.type() == RelationshipType.ManyToMany) {
                // Source side: ManyToMany
                relationships.add(new JHipsterRelationship(
                    JHipsterRelationshipType.ManyToMany,
                    node.sourceEntity(),
                    node.sourceProperty(),
                    node.targetEntity(),
                    node.targetProperty()
                ));

                // Target side: ManyToMany (bidirectional if targetProperty is present)
                if (node.targetProperty().isPresent()) {
                    relationships.add(new JHipsterRelationship(
                        JHipsterRelationshipType.ManyToMany,
                        node.targetEntity(),
                        node.targetProperty(),
                        node.sourceEntity(),
                        node.sourceProperty()
                    ));
                }
            }
        }

        return List.copyOf(relationships);
    }

    /**
     * Converts a single CDL relationship node into JHipsterRelationship metadata.
     * Maps the primary direction directly.
     *
     * @param node the CDL relationship node
     * @return the primary JHipster relationship
     */
    public JHipsterRelationship mapDirect(RelationshipNode node) {
        if (node == null) {
            throw new IllegalArgumentException("RelationshipNode cannot be null");
        }

        JHipsterRelationshipType type;
        switch (node.type()) {
            case OneToOne -> type = JHipsterRelationshipType.OneToOne;
            case OneToMany -> type = JHipsterRelationshipType.OneToMany;
            case ManyToMany -> type = JHipsterRelationshipType.ManyToMany;
            default -> throw new IllegalArgumentException("Unsupported relationship type: " + node.type());
        }

        return new JHipsterRelationship(
            type,
            node.sourceEntity(),
            node.sourceProperty(),
            node.targetEntity(),
            node.targetProperty()
        );
    }
}
