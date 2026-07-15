package uk.ac.bham.codeclassroom.generator.inheritance;

import uk.ac.bham.codeclassroom.generator.ast.*;

import java.util.*;

/**
 * Resolves the inheritance tree of a CDL CompilationUnit, computes depths,
 * detects circular references, and outputs a topologically-sorted ResolvedInheritanceModel.
 */
public class InheritanceResolver {

    /**
     * Resolves and builds the inheritance tree model.
     *
     * @param compilationUnit parsed AST root
     * @return a complete ResolvedInheritanceModel containing topological entities
     * @throws InheritanceResolutionException if cycle, missing references, or other errors are found
     */
    public ResolvedInheritanceModel resolve(CompilationUnit compilationUnit) {
        if (compilationUnit == null) {
            return new ResolvedInheritanceModel(List.of(), Map.of(), List.of());
        }

        List<EntityNode> entities = compilationUnit.entities();
        int totalEntities = entities.size();

        // 1. Build parent, child, and entity name maps in O(n)
        Map<String, String> parentMap = new HashMap<>(); // child -> parent
        Map<String, List<String>> childMap = new HashMap<>(); // parent -> list of children
        Map<String, EntityNode> entityMap = new HashMap<>(); // name -> entity node

        for (EntityNode entity : entities) {
            String name = entity.name();
            entityMap.put(name, entity);
            childMap.put(name, new ArrayList<>());

            if (entity.inheritance().isPresent()) {
                String parent = entity.inheritance().get().parentName();
                parentMap.put(name, parent);
            }
        }

        // Map children to parent records, throwing on dangling parent references
        for (Map.Entry<String, String> entry : parentMap.entrySet()) {
            String child = entry.getKey();
            String parent = entry.getValue();
            if (childMap.containsKey(parent)) {
                childMap.get(parent).add(child);
            } else {
                throw new InheritanceResolutionException(String.format(
                        "Dangling inheritance: entity '%s' references unknown parent '%s'.", child, parent
                ));
            }
        }

        // 2. Defensive Cycle Detection using O(n) 3-color DFS
        checkForCycles(entities, parentMap);

        // 3. Identify all Root Entities (depth = 0)
        List<String> rootNames = new ArrayList<>();
        for (EntityNode entity : entities) {
            if (!parentMap.containsKey(entity.name())) {
                rootNames.add(entity.name());
            }
        }

        // Sort rootNames alphabetically for deterministic output ordering among roots
        rootNames.sort(String::compareTo);

        // 4. Traverse hierarchy using BFS to build topological generation order and calculate depths
        List<ResolvedEntity> orderedEntities = new ArrayList<>();
        Map<String, ResolvedEntity> lookupByName = new HashMap<>();
        List<ResolvedEntity> rootEntities = new ArrayList<>();

        Queue<TraversalNode> queue = new LinkedList<>();
        for (String rootName : rootNames) {
            queue.add(new TraversalNode(rootName, 0));
        }

        int generationOrderCounter = 0;

        while (!queue.isEmpty()) {
            TraversalNode current = queue.poll();
            String name = current.name;
            int depth = current.depth;

            String parent = parentMap.get(name);
            List<String> children = childMap.get(name);
            // Sort subclass names alphabetically for deterministic child ordering
            children.sort(String::compareTo);
            List<String> immutableChildren = List.copyOf(children);

            boolean requiresInheritanceAnnotation = !immutableChildren.isEmpty();
            boolean requiresExtends = parent != null;

            ResolvedEntity resolved = new ResolvedEntity(
                name,
                Optional.ofNullable(parent),
                immutableChildren,
                depth,
                generationOrderCounter++,
                requiresInheritanceAnnotation,
                requiresExtends
            );

            orderedEntities.add(resolved);
            lookupByName.put(name, resolved);

            if (resolved.isRoot()) {
                rootEntities.add(resolved);
            }

            // Enqueue direct subclasses at depth + 1
            for (String child : immutableChildren) {
                queue.add(new TraversalNode(child, depth + 1));
            }
        }

        // 5. Verification: guarantee all defined entities were resolved
        if (orderedEntities.size() < totalEntities) {
            throw new InheritanceResolutionException("Dangling references or cycles prevented full topological resolution.");
        }

        return new ResolvedInheritanceModel(
            List.copyOf(orderedEntities),
            Map.copyOf(lookupByName),
            List.copyOf(rootEntities)
        );
    }

    private void checkForCycles(List<EntityNode> entities, Map<String, String> parentMap) {
        // state: 0=unvisited, 1=visiting, 2=visited
        Map<String, Integer> state = new HashMap<>();
        for (EntityNode entity : entities) {
            state.put(entity.name(), 0);
        }

        for (EntityNode entity : entities) {
            if (state.get(entity.name()) == 0) {
                dfsCycleCheck(entity.name(), parentMap, state);
            }
        }
    }

    private void dfsCycleCheck(String name, Map<String, String> parentMap, Map<String, Integer> state) {
        state.put(name, 1); // Mark as visiting

        String parent = parentMap.get(name);
        if (parent != null) {
            Integer parentState = state.get(parent);
            if (parentState != null) {
                if (parentState == 1) {
                    throw new InheritanceResolutionException("Circular inheritance cycle detected involving: " + name);
                } else if (parentState == 0) {
                    dfsCycleCheck(parent, parentMap, state);
                }
            }
        }

        state.put(name, 2); // Mark as fully visited
    }

    private record TraversalNode(String name, int depth) {}
}
