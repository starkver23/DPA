package uk.ac.bham.codeclassroom.generator.inheritance;

import java.util.List;
import java.util.Optional;

/**
 * Immutable metadata representing a resolved entity in the inheritance hierarchy.
 *
 * @param entityName                    the name of the entity
 * @param parentName                    the direct superclass name (empty if root)
 * @param children                      direct subclasses list
 * @param depth                         depth in the hierarchy (root = 0, subclasses = parent depth + 1)
 * @param generationOrder               topological ordering number (parents before children)
 * @param requiresInheritanceAnnotation true if this is a root or intermediate entity with subclasses
 * @param requiresExtends               true if this entity extends another class (leaf/intermediate)
 */
public record ResolvedEntity(
    String entityName,
    Optional<String> parentName,
    List<String> children,
    int depth,
    int generationOrder,
    boolean requiresInheritanceAnnotation,
    boolean requiresExtends
) {
    public boolean isRoot() {
        return parentName.isEmpty();
    }

    public boolean hasParent() {
        return parentName.isPresent();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}
