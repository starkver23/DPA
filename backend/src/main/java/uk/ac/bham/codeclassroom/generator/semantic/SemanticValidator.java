package uk.ac.bham.codeclassroom.generator.semantic;

import uk.ac.bham.codeclassroom.generator.ast.*;

import java.util.*;

/**
 * Validates the Abstract Syntax Tree (AST) of a CDL file for logical and semantic correctness.
 */
public class SemanticValidator {

    private static final Set<String> PRIMITIVES = Set.of(
            "String", "Integer", "Long", "Boolean", "BigDecimal", "LocalDate"
    );

    private static final Set<String> GENERIC_CONTAINERS = Set.of(
            "List", "Set", "Map", "Collection"
    );

    /**
     * Performs semantic validation on the provided CompilationUnit.
     * Throws SemanticException if any rule is violated.
     *
     * @param compilationUnit the root AST node
     * @throws SemanticException if validation fails
     */
    public void validate(CompilationUnit compilationUnit) {
        if (compilationUnit == null) {
            return;
        }

        Set<String> knownEntities = validateEntityNamesAndDuplicates(compilationUnit.entities());
        validateInheritance(compilationUnit.entities(), knownEntities);
        validateEntityMembers(compilationUnit.entities(), knownEntities);
        validateRelationships(compilationUnit.relationships(), knownEntities);
    }

    private Set<String> validateEntityNamesAndDuplicates(List<EntityNode> entities) {
        Set<String> knownEntities = new HashSet<>();
        for (EntityNode entity : entities) {
            if (!knownEntities.add(entity.name())) {
                throw new SemanticException(String.format("Duplicate entity '%s'.", entity.name()));
            }
        }
        return knownEntities;
    }

    private void validateInheritance(List<EntityNode> entities, Set<String> knownEntities) {
        Map<String, String> inheritanceMap = new HashMap<>();

        for (EntityNode entity : entities) {
            if (entity.inheritance().isPresent()) {
                String parentName = entity.inheritance().get().parentName();

                // 4. Unknown parent entity
                if (!knownEntities.contains(parentName)) {
                    throw new SemanticException(String.format("Unknown parent entity '%s'.", parentName));
                }

                // 5. Self inheritance
                if (parentName.equals(entity.name())) {
                    throw new SemanticException(String.format("Entity '%s' cannot extend itself.", entity.name()));
                }

                inheritanceMap.put(entity.name(), parentName);
            }
        }

        // 6. Circular inheritance
        for (String entityName : inheritanceMap.keySet()) {
            Set<String> visited = new LinkedHashSet<>();
            String current = entityName;
            while (current != null) {
                if (visited.contains(current)) {
                    // Build path description: current -> A -> B -> current
                    List<String> list = new ArrayList<>(visited);
                    int startIdx = list.indexOf(current);
                    StringBuilder path = new StringBuilder();
                    for (int i = startIdx; i < list.size(); i++) {
                        path.append(list.get(i)).append(" -> ");
                    }
                    path.append(current);

                    throw new SemanticException(String.format(
                            "Circular inheritance detected: %s.", path
                    ));
                }
                visited.add(current);
                current = inheritanceMap.get(current);
            }
        }
    }

    private void validateEntityMembers(List<EntityNode> entities, Set<String> knownEntities) {
        for (EntityNode entity : entities) {
            Set<String> fieldNames = new HashSet<>();
            for (FieldNode field : entity.fields()) {
                // 2. Duplicate fields
                if (!fieldNames.add(field.name())) {
                    throw new SemanticException(String.format(
                            "Duplicate field '%s' in entity '%s'.", field.name(), entity.name()
                    ));
                }
                // 10. Invalid types
                validateTypeReference(field.type(), knownEntities, entity.name(), "field '" + field.name() + "'");
            }

            Set<String> methodSignatures = new HashSet<>();
            for (MethodNode method : entity.methods()) {
                String signature = getMethodSignature(method);
                // 3. Duplicate methods
                if (!methodSignatures.add(signature)) {
                    throw new SemanticException(String.format(
                            "Duplicate method '%s' in entity '%s'.", signature, entity.name()
                    ));
                }

                // Validate parameters
                for (ParameterNode param : method.parameters()) {
                    validateTypeReference(param.type(), knownEntities, entity.name(), "parameter '" + param.name() + "' of method '" + method.name() + "'");
                }

                // Validate return type
                if (method.returnType().isPresent()) {
                    validateTypeReference(method.returnType().get(), knownEntities, entity.name(), "return type of method '" + method.name() + "'");
                }
            }
        }
    }

    private void validateTypeReference(TypeReference type, Set<String> knownEntities, String entityName, String context) {
        if (type.genericType().isPresent()) {
            // Validate generic container
            if (!GENERIC_CONTAINERS.contains(type.baseType())) {
                throw new SemanticException(String.format(
                        "Unsupported generic type container '%s' in %s of entity '%s'.",
                        type.baseType(), context, entityName
                ));
            }
            // Validate inner type
            validateTypeReference(type.genericType().get(), knownEntities, entityName, context);
        } else {
            // Validate simple type
            String base = type.baseType();
            if (!PRIMITIVES.contains(base) && !knownEntities.contains(base)) {
                throw new SemanticException(String.format(
                        "Unknown type '%s' referenced in %s of entity '%s'.",
                        base, context, entityName
                ));
            }
        }
    }

    private String getMethodSignature(MethodNode method) {
        StringBuilder sb = new StringBuilder(method.name()).append("(");
        for (int i = 0; i < method.parameters().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(method.parameters().get(i).type().toString());
        }
        sb.append(")");
        return sb.toString();
    }

    private void validateRelationships(List<RelationshipNode> relationships, Set<String> knownEntities) {
        Set<String> relationshipKeys = new HashSet<>();

        for (RelationshipNode rel : relationships) {
            // 7. Invalid relationship entities
            if (!knownEntities.contains(rel.sourceEntity())) {
                throw new SemanticException(String.format(
                        "Unknown relationship source entity '%s'.", rel.sourceEntity()
                ));
            }
            if (!knownEntities.contains(rel.targetEntity())) {
                throw new SemanticException(String.format(
                        "Unknown relationship target entity '%s'.", rel.targetEntity()
                ));
            }

            // 9. Relationship self-reference
            if (rel.sourceEntity().equals(rel.targetEntity())) {
                throw new SemanticException(String.format(
                        "Relationship self-reference is not allowed: '%s to %s'.",
                        rel.sourceEntity(), rel.targetEntity()
                ));
            }

            // 8. Duplicate relationship
            String key = getRelationshipKey(rel);
            if (!relationshipKeys.add(key)) {
                throw new SemanticException("Duplicate relationship detected.");
            }
        }
    }

    private String getRelationshipKey(RelationshipNode rel) {
        return String.format("%s:%s{%s} to %s{%s}",
                rel.type(),
                rel.sourceEntity(),
                rel.sourceProperty().orElse(""),
                rel.targetEntity(),
                rel.targetProperty().orElse("")
        );
    }
}
