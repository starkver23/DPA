package uk.ac.bham.codeclassroom.generator.ast;

import java.util.List;
import java.util.Optional;

/**
 * Represents an entity or interface block in the CDL grammar.
 *
 * @param name                 the name of the entity
 * @param inheritance          the optional class inheritance descriptor (extends)
 * @param abstractClass        whether this class is abstract
 * @param fields               the list of fields defined on the entity
 * @param methods              the list of methods declared on the entity
 * @param kind                 the kind of the entity (CLASS or INTERFACE)
 * @param implementsInterfaces the list of interface names implemented by this class
 * @param extendsInterfaces    the list of interface names extended by this interface
 */
public record EntityNode(
    String name,
    Optional<InheritanceNode> inheritance,
    boolean abstractClass,
    List<FieldNode> fields,
    List<MethodNode> methods,
    EntityKind kind,
    List<String> implementsInterfaces,
    List<String> extendsInterfaces
) {
    /**
     * Backward-compatible constructor for standard class entities.
     */
    public EntityNode(
        String name,
        Optional<InheritanceNode> inheritance,
        boolean abstractClass,
        List<FieldNode> fields,
        List<MethodNode> methods
    ) {
        this(
            name,
            inheritance,
            abstractClass,
            fields,
            methods,
            EntityKind.CLASS,
            List.of(),
            List.of()
        );
    }
}
