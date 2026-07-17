package uk.ac.bham.codeclassroom.generator.jhipster;

import java.util.List;

/**
 * Complete JHipster intermediate metadata model for the project.
 *
 * @param entities      all entities in the project
 * @param relationships all relationships in the project
 * @param configuration the configuration for the project
 */
public record JHipsterProject(
    List<JHipsterEntity> entities,
    List<JHipsterRelationship> relationships,
    JHipsterProjectConfiguration configuration
) {}
