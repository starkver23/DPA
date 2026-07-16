package uk.ac.bham.codeclassroom.generator.ast;

import java.util.List;
import java.util.Optional;

/**
 * Represents an entity block in the CDL grammar.
 *
 * @param name        the name of the entity
 * @param inheritance the optional inheritance descriptor
 * @param fields      the list of fields defined on the entity
 * @param methods     the list of methods declared on the entity
 */
public record EntityNode(
    String name,
    Optional<InheritanceNode> inheritance,
    List<FieldNode> fields,
    List<MethodNode> methods
) {}
