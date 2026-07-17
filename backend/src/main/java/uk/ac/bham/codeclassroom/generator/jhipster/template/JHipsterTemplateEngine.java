package uk.ac.bham.codeclassroom.generator.jhipster.template;

import uk.ac.bham.codeclassroom.generator.engine.GeneratedFile;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedProject;
import uk.ac.bham.codeclassroom.generator.engine.TemplateRenderer;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterEntity;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the generation of inheritance-aware JHipster components.
 */
public class JHipsterTemplateEngine {

    private final TemplateRenderer renderer = new TemplateRenderer();
    private final TemplateModelBuilder modelBuilder = new TemplateModelBuilder();

    /**
     * Translates a JHipsterProject metadata object into an in-memory GeneratedProject.
     *
     * @param project the JHipsterProject intermediate metadata project
     * @return the generated JHipster-compatible Java project holding in-memory file templates
     */
    public GeneratedProject generate(JHipsterProject project) {
        if (project == null) {
            throw new IllegalArgumentException("JHipsterProject cannot be null");
        }

        List<GeneratedFile> generatedFiles = new ArrayList<>();

        for (JHipsterEntity entity : project.entities()) {
            // Build the scope model using TemplateModelBuilder
            TemplateGenerationContext context = modelBuilder.build(entity, project);

            // 1. Entity
            String entityContent = renderer.render("templates/jhipster/Entity.mustache", context);
            generatedFiles.add(new GeneratedFile(
                entity.entityName() + ".java",
                "src/main/java/" + project.configuration().basePackage().replace('.', '/') + "/domain/" + entity.entityName() + ".java",
                entityContent
            ));

            // 2. Repository
            String repositoryContent = renderer.render("templates/jhipster/Repository.mustache", context);
            generatedFiles.add(new GeneratedFile(
                entity.entityName() + "Repository.java",
                "src/main/java/" + project.configuration().basePackage().replace('.', '/') + "/repository/" + entity.entityName() + "Repository.java",
                repositoryContent
            ));

            // 3. Service
            String serviceContent = renderer.render("templates/jhipster/Service.mustache", context);
            generatedFiles.add(new GeneratedFile(
                entity.entityName() + "Service.java",
                "src/main/java/" + project.configuration().basePackage().replace('.', '/') + "/service/" + entity.entityName() + "Service.java",
                serviceContent
            ));

            // 4. Service Implementation
            String serviceImplContent = renderer.render("templates/jhipster/ServiceImpl.mustache", context);
            generatedFiles.add(new GeneratedFile(
                entity.entityName() + "ServiceImpl.java",
                "src/main/java/" + project.configuration().basePackage().replace('.', '/') + "/service/impl/" + entity.entityName() + "ServiceImpl.java",
                serviceImplContent
            ));

            // 5. Controller (Resource in JHipster context)
            String controllerContent = renderer.render("templates/jhipster/Controller.mustache", context);
            generatedFiles.add(new GeneratedFile(
                entity.entityName() + "Resource.java",
                "src/main/java/" + project.configuration().basePackage().replace('.', '/') + "/web/rest/" + entity.entityName() + "Resource.java",
                controllerContent
            ));

            // 6. DTO
            String dtoContent = renderer.render("templates/jhipster/DTO.mustache", context);
            generatedFiles.add(new GeneratedFile(
                entity.entityName() + "DTO.java",
                "src/main/java/" + project.configuration().basePackage().replace('.', '/') + "/service/dto/" + entity.entityName() + "DTO.java",
                dtoContent
            ));

            // 7. Mapper
            String mapperContent = renderer.render("templates/jhipster/Mapper.mustache", context);
            generatedFiles.add(new GeneratedFile(
                entity.entityName() + "Mapper.java",
                "src/main/java/" + project.configuration().basePackage().replace('.', '/') + "/service/mapper/" + entity.entityName() + "Mapper.java",
                mapperContent
            ));
        }

        return new GeneratedProject(List.copyOf(generatedFiles));
    }
}
