package uk.ac.bham.codeclassroom.generator.ast;

import java.util.List;
import java.util.Optional;

/**
 * Represents a method signature inside an entity block.
 * Examples: login(), getName(): String, calculateSalary(Integer hours)
 *
 * @param name       the name of the method
 * @param parameters the ordered list of parameters
 * @param returnType the optional return type
 */
public record MethodNode(
    String name,
    List<ParameterNode> parameters,
    Optional<TypeReference> returnType
) {}
