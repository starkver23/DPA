package uk.ac.bham.codeclassroom.generator.inheritance;

import uk.ac.bham.codeclassroom.generator.ast.EntityNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the fully resolved inheritance metadata for the entire compilation unit,
 * preserving topologically ordered sequences and fast symbol lookups.
 *
 * @param orderedEntities Topological list of entities, parents always before children
 * @param lookupByName    Map of entity names to their metadata
 * @param rootEntities    List of all root entities (depth = 0)
 */
public record ResolvedInheritanceModel(
    List<ResolvedEntity> orderedEntities,
    Map<String, ResolvedEntity> lookupByName,
    List<ResolvedEntity> rootEntities,
    Map<String, EntityNode> entityNodes
) {
    /**
     * Finds resolved entity metadata by its name.
     *
     * @param name name of the entity to lookup
     * @return an Optional enclosing the ResolvedEntity if found
     */
    public Optional<ResolvedEntity> findByName(String name) {
        return Optional.ofNullable(lookupByName.get(name));
    }
}
