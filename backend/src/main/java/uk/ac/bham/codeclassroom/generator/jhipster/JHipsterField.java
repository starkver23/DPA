package uk.ac.bham.codeclassroom.generator.jhipster;

import uk.ac.bham.codeclassroom.generator.ast.TypeReference;

/**
 * Immutable metadata representing a field in a JHipster entity.
 *
 * @param name the name of the field
 * @param type the type reference of the field
 */
public record JHipsterField(
    String name,
    TypeReference type
) {}
