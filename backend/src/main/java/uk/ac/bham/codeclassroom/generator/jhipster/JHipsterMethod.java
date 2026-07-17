package uk.ac.bham.codeclassroom.generator.jhipster;

import uk.ac.bham.codeclassroom.generator.ast.TypeReference;

import java.util.List;
import java.util.Optional;

/**
 * Immutable metadata representing a method in a JHipster entity.
 *
 * @param name       the name of the method
 * @param parameters the list of parameters for the method
 * @param returnType the optional return type reference of the method
 */
public record JHipsterMethod(
    String name,
    List<JHipsterParameter> parameters,
    Optional<TypeReference> returnType
) {}
