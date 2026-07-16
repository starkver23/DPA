package uk.ac.bham.codeclassroom.generator.engine;

import uk.ac.bham.codeclassroom.generator.ast.EntityNode;
import uk.ac.bham.codeclassroom.generator.ast.FieldNode;
import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedEntity;
import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedInheritanceModel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the generation of clean, JPA-compliant, layered Spring Boot components in memory.
 */
public class CodeGenerationEngine {

    private final TemplateRenderer renderer = new TemplateRenderer();

    /**
     * Translates a resolved inheritance model into an in-memory GeneratedProject.
     *
     * @param model the resolved entity and hierarchy metadata
     * @return the generated Java project holding in-memory file templates
     */
    public GeneratedProject generate(ResolvedInheritanceModel model) {
        if (model == null) {
            return new GeneratedProject(List.of());
        }

        List<GeneratedFile> generatedFiles = new ArrayList<>();

        for (ResolvedEntity resolved : model.orderedEntities()) {
            EntityNode astNode = model.entityNodes().get(resolved.entityName());
            List<FieldNode> astFields = astNode != null ? astNode.fields() : List.of();

            // Prepare scope context for Mustache
            Map<String, Object> context = new HashMap<>();
            context.put("className", resolved.entityName());
            context.put("parentClassName", resolved.parentName().orElse(""));
            context.put("requiresInheritanceAnnotation", resolved.requiresInheritanceAnnotation());
            context.put("requiresExtends", resolved.requiresExtends());
            
            // Format plural mapping (e.g. Student -> students)
            String pluralLowerName = resolved.entityName().toLowerCase() + "s";
            context.put("pluralLowerName", pluralLowerName);

            // Structure field properties dynamically
            List<Map<String, String>> fields = astFields.stream().map(field -> {
                Map<String, String> f = new HashMap<>();
                f.put("name", field.name());
                f.put("type", field.type().toString());
                
                String cap = field.name().substring(0, 1).toUpperCase() + field.name().substring(1);
                f.put("capitalizedName", cap);
                return f;
            }).collect(Collectors.toList());
            context.put("fields", fields);

            // 1. Entity
            String entityContent = renderer.render("templates/Entity.mustache", context);
            generatedFiles.add(new GeneratedFile(
                    resolved.entityName() + ".java",
                    "src/main/java/uk/ac/bham/codeclassroom/model/" + resolved.entityName() + ".java",
                    entityContent
            ));

            // 2. Repository
            String repositoryContent = renderer.render("templates/Repository.mustache", context);
            generatedFiles.add(new GeneratedFile(
                    resolved.entityName() + "Repository.java",
                    "src/main/java/uk/ac/bham/codeclassroom/repository/" + resolved.entityName() + "Repository.java",
                    repositoryContent
            ));

            // 3. Service
            String serviceContent = renderer.render("templates/Service.mustache", context);
            generatedFiles.add(new GeneratedFile(
                    resolved.entityName() + "Service.java",
                    "src/main/java/uk/ac/bham/codeclassroom/service/" + resolved.entityName() + "Service.java",
                    serviceContent
            ));

            // 4. Service Implementation
            String serviceImplContent = renderer.render("templates/ServiceImpl.mustache", context);
            generatedFiles.add(new GeneratedFile(
                    resolved.entityName() + "ServiceImpl.java",
                    "src/main/java/uk/ac/bham/codeclassroom/service/impl/" + resolved.entityName() + "ServiceImpl.java",
                    serviceImplContent
            ));

            // 5. Controller
            String controllerContent = renderer.render("templates/Controller.mustache", context);
            generatedFiles.add(new GeneratedFile(
                    resolved.entityName() + "Controller.java",
                    "src/main/java/uk/ac/bham/codeclassroom/controller/" + resolved.entityName() + "Controller.java",
                    controllerContent
            ));

            // 6. DTO
            String dtoContent = renderer.render("templates/DTO.mustache", context);
            generatedFiles.add(new GeneratedFile(
                    resolved.entityName() + "DTO.java",
                    "src/main/java/uk/ac/bham/codeclassroom/dto/" + resolved.entityName() + "DTO.java",
                    dtoContent
            ));

            // 7. Mapper
            String mapperContent = renderer.render("templates/Mapper.mustache", context);
            generatedFiles.add(new GeneratedFile(
                    resolved.entityName() + "Mapper.java",
                    "src/main/java/uk/ac/bham/codeclassroom/mapper/" + resolved.entityName() + "Mapper.java",
                    mapperContent
            ));
        }

        return new GeneratedProject(List.copyOf(generatedFiles));
    }
}
