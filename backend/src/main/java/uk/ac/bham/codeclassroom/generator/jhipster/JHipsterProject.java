package uk.ac.bham.codeclassroom.generator.jhipster;

import uk.ac.bham.codeclassroom.generator.ast.EntityNode;
import java.util.List;

/**
 * Complete JHipster intermediate metadata model for the project.
 *
 * @param entities         all entities in the project
 * @param relationships    all relationships in the project
 * @param configuration    the configuration for the project
 * @param interfaces       all interface AST nodes in the project
 * @param originalEntities all original entity and interface AST nodes in the project
 */
public record JHipsterProject(
    List<JHipsterEntity> entities,
    List<JHipsterRelationship> relationships,
    JHipsterProjectConfiguration configuration,
    List<EntityNode> interfaces,
    List<EntityNode> originalEntities
) {
    /**
     * Backward-compatible constructor.
     */
    public JHipsterProject(
        List<JHipsterEntity> entities,
        List<JHipsterRelationship> relationships,
        JHipsterProjectConfiguration configuration
    ) {
        this(entities, relationships, configuration, List.of(), List.of());
    }
}
