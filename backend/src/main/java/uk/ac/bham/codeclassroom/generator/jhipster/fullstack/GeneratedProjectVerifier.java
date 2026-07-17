package uk.ac.bham.codeclassroom.generator.jhipster.fullstack;

import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jdl.JDLInheritance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates that a generated JHipster project contains all standard JHipster full-stack layers
 * and correctly applied CodeClassroom inheritance modifications.
 */
public class GeneratedProjectVerifier {

    /**
     * Verifies that the generated project is complete and valid.
     *
     * @param projectPath the root directory path of the generated JHipster project
     * @param document    the ExtendedJDLDocument containing configuration and metadata
     * @throws FullStackGenerationException if any verification check fails
     */
    public void verify(Path projectPath, ExtendedJDLDocument document) {
        if (projectPath == null || document == null) {
            throw new FullStackGenerationException("Project path and ExtendedJDLDocument cannot be null");
        }

        if (!Files.exists(projectPath)) {
            throw new FullStackGenerationException("Project directory does not exist: " + projectPath);
        }

        String basePackage = document.configuration().basePackage();
        String packagePathStr = "src/main/java/" + basePackage.replace('.', '/');
        Path packagePath = projectPath.resolve(packagePathStr);

        // 1. Verify Backend directories exist
        verifyDirectory(packagePath.resolve("domain"), "Spring Boot Domain directory");
        verifyDirectory(packagePath.resolve("repository"), "Spring Boot Repository directory");
        verifyDirectory(packagePath.resolve("service"), "Spring Boot Service directory");
        verifyDirectory(packagePath.resolve("service/dto"), "Spring Boot DTO directory");
        verifyDirectory(packagePath.resolve("service/mapper"), "Spring Boot Mapper directory");
        verifyDirectory(packagePath.resolve("web/rest"), "Spring Boot Web/REST Controller directory");

        // 2. Verify Frontend directory and artifacts exist
        Path webappPath = projectPath.resolve("src/main/webapp");
        verifyDirectory(webappPath, "React frontend root directory (src/main/webapp)");
        verifyDirectory(webappPath.resolve("app/entities"), "React entity CRUD page directory");

        // 3. Verify Database (Liquibase) files exist
        verifyDirectory(projectPath.resolve("src/main/resources/config/liquibase"), "Liquibase database changelogs directory");

        // 4. Verify Docker Compose files exist
        verifyDirectory(projectPath.resolve("src/main/docker"), "Docker Compose configuration directory");

        // 5. Verify i18n resources exist
        verifyDirectory(projectPath.resolve("src/main/resources/i18n"), "JHipster i18n resources directory");

        // 6. Verify authentication configurations exist
        Path securityConfig = packagePath.resolve("config/SecurityConfiguration.java");
        if (!Files.exists(packagePath.resolve("config")) && !Files.exists(securityConfig)) {
            verifyDirectory(packagePath.resolve("config"), "Spring Boot Configuration directory");
        }

        // 7. Verify Inheritance Modifications are applied in files
        for (JDLInheritance inh : document.inheritanceDeclarations()) {
            Path childEntityFile = packagePath.resolve("domain/" + inh.childEntity() + ".java");
            if (Files.exists(childEntityFile)) {
                try {
                    String content = Files.readString(childEntityFile);
                    if (!content.contains("extends " + inh.parentEntity())) {
                        throw new FullStackGenerationException("Entity " + inh.childEntity() + " is missing inheritance extends signature.");
                    }
                } catch (IOException e) {
                    throw new FullStackGenerationException("Failed to read entity file during verification: " + childEntityFile, e);
                }
            }

            Path childDtoFile = packagePath.resolve("service/dto/" + inh.childEntity() + "DTO.java");
            if (Files.exists(childDtoFile)) {
                try {
                    String content = Files.readString(childDtoFile);
                    if (!content.contains("extends " + inh.parentEntity() + "DTO")) {
                        throw new FullStackGenerationException("DTO " + inh.childEntity() + "DTO is missing inheritance extends signature.");
                    }
                } catch (IOException e) {
                    throw new FullStackGenerationException("Failed to read DTO file during verification: " + childDtoFile, e);
                }
            }

            Path parentEntityFile = packagePath.resolve("domain/" + inh.parentEntity() + ".java");
            if (Files.exists(parentEntityFile)) {
                try {
                    String content = Files.readString(parentEntityFile);

                    boolean parentIsChild = document.inheritanceDeclarations().stream()
                        .anyMatch(i -> i.childEntity().equals(inh.parentEntity()));

                    if (!parentIsChild && !content.contains("@Inheritance")) {
                        throw new FullStackGenerationException("Root entity " + inh.parentEntity() + " is missing @Inheritance JPA strategy annotation.");
                    }
                } catch (IOException e) {
                    throw new FullStackGenerationException("Failed to read parent entity file during verification: " + parentEntityFile, e);
                }
            }
        }
    }

    private void verifyDirectory(Path path, String description) {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new FullStackGenerationException("Missing expected full-stack component: " + description + " at: " + path);
        }
    }
}
