package uk.ac.bham.codeclassroom.generator.ast;

import java.util.Optional;

/**
 * Represents a reference to a type, supporting both simple types (e.g. String, Integer)
 * and generic types (e.g. List&lt;Course&gt;).
 *
 * @param baseType    the name of the base class or primitive type
 * @param genericType the optional generic type parameter
 */
public record TypeReference(
    String baseType,
    Optional<TypeReference> genericType
) {
    @Override
    public String toString() {
        return genericType
                .map(g -> baseType + "<" + g + ">")
                .orElse(baseType);
    }
}
