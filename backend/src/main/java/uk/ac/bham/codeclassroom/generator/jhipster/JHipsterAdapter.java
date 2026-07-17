package uk.ac.bham.codeclassroom.generator.jhipster;

import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.ast.EntityNode;
import uk.ac.bham.codeclassroom.generator.inheritance.InheritanceResolver;
import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedInheritanceModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main orchestration class for the JHipster Metadata Adapter Layer.
 * Adapts the parsed compilation unit AST into a JHipster intermediate metadata project representation.
 */
public class JHipsterAdapter {

    private final EntityMetadataGenerator entityGenerator;
    private final RelationshipMetadataGenerator relationshipGenerator;
    private final InheritanceMetadataGenerator inheritanceGenerator;

    /**
     * Default constructor initializing all sub-generators.
     */
    public JHipsterAdapter() {
        this.entityGenerator = new EntityMetadataGenerator();
        this.relationshipGenerator = new RelationshipMetadataGenerator();
        this.inheritanceGenerator = new InheritanceMetadataGenerator();
    }

    /**
     * Constructor allowing injection of generators for testing or extensibility.
     *
     * @param entityGenerator       the entity generator to use
     * @param relationshipGenerator the relationship generator to use
     * @param inheritanceGenerator  the inheritance generator to use
     */
    public JHipsterAdapter(
        EntityMetadataGenerator entityGenerator,
        RelationshipMetadataGenerator relationshipGenerator,
        InheritanceMetadataGenerator inheritanceGenerator
    ) {
        this.entityGenerator = entityGenerator;
        this.relationshipGenerator = relationshipGenerator;
        this.inheritanceGenerator = inheritanceGenerator;
    }

    /**
     * Adapts the CDL AST into a JHipsterProject metadata model using the provided configuration.
     *
     * @param compilationUnit the parsed compilation unit AST
     * @param configuration   the configuration for the JHipster project
     * @return the adapted JHipsterProject metadata representation
     */
    public JHipsterProject adapt(CompilationUnit compilationUnit, JHipsterProjectConfiguration configuration) {
        if (compilationUnit == null) {
            throw new IllegalArgumentException("CompilationUnit cannot be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("JHipsterProjectConfiguration cannot be null");
        }

        // 1. Resolve inheritance using the existing InheritanceResolver
        InheritanceResolver inheritanceResolver = new InheritanceResolver();
        ResolvedInheritanceModel resolvedInheritance = inheritanceResolver.resolve(compilationUnit);

        // 2. Generate all relationship metadata
        List<JHipsterRelationship> allRelationships = relationshipGenerator.generate(compilationUnit.relationships());

        // 3. For each entity, generate inheritance and filter/associate relationships, then generate JHipsterEntity
        List<JHipsterEntity> entities = new ArrayList<>();
        for (EntityNode entityNode : compilationUnit.entities()) {
            String entityName = entityNode.name();

            // Resolve inheritance metadata for this entity
            JHipsterInheritance inheritanceMetadata = inheritanceGenerator.generate(entityName, resolvedInheritance);

            // Filter relationships where this entity is the source
            List<JHipsterRelationship> entityRelationships = allRelationships.stream()
                .filter(r -> r.sourceEntity().equals(entityName))
                .collect(Collectors.toList());

            // Generate JHipsterEntity
            JHipsterEntity jhipsterEntity = entityGenerator.generate(entityNode, inheritanceMetadata, entityRelationships);
            entities.add(jhipsterEntity);
        }

        return new JHipsterProject(
            List.copyOf(entities),
            allRelationships,
            configuration
        );
    }
}
