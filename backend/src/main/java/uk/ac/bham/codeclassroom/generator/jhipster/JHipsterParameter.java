package uk.ac.bham.codeclassroom.generator.jhipster;

import uk.ac.bham.codeclassroom.generator.ast.TypeReference;

/**
 * Immutable metadata representing a method parameter in a JHipster entity.
 *
 * @param name the name of the parameter
 * @param type the type reference of the parameter
 */
public record JHipsterParameter(
    String name,
    TypeReference type
) {}
