package uk.ac.bham.codeclassroom.generator.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Encapsulates the directories structure of a Spring Boot project.
 */
public class ProjectStructure {
    private final Path projectRoot;
    private final ProjectMetadata metadata;

    /**
     * Constructs ProjectStructure and performs initial validations.
     *
     * @param outputDirectory root output folder
     * @param metadata        project metadata specifications
     */
    public ProjectStructure(Path outputDirectory, ProjectMetadata metadata) {
        if (outputDirectory == null) {
            throw new ProjectBuilderException("Output directory path cannot be null");
        }
        if (metadata == null) {
            throw new ProjectBuilderException("Project metadata cannot be null");
        }
        if (metadata.projectName() == null || metadata.projectName().isBlank()) {
            throw new ProjectBuilderException("Project name is missing in metadata");
        }
        if (metadata.packageName() == null || metadata.packageName().isBlank()) {
            throw new ProjectBuilderException("Package name is missing in metadata");
        }

        this.projectRoot = outputDirectory.resolve(metadata.projectName());
        this.metadata = metadata;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Path getJavaSourceDir() {
        String packagePath = metadata.packageName().replace('.', '/');
        return projectRoot.resolve("src/main/java").resolve(packagePath);
    }

    public Path getResourcesDir() {
        return projectRoot.resolve("src/main/resources");
    }

    /**
     * Creates all the necessary folders for the Spring Boot Maven structure.
     */
    public void createDirectories() {
        try {
            // Create root project folder
            Files.createDirectories(projectRoot);

            // Create java source directories
            Path javaSource = getJavaSourceDir();
            Files.createDirectories(javaSource);
            Files.createDirectories(javaSource.resolve("model"));
            Files.createDirectories(javaSource.resolve("repository"));
            Files.createDirectories(javaSource.resolve("service/impl"));
            Files.createDirectories(javaSource.resolve("controller"));
            Files.createDirectories(javaSource.resolve("dto"));
            Files.createDirectories(javaSource.resolve("mapper"));

            // Create resources directory
            Files.createDirectories(getResourcesDir());
        } catch (IOException e) {
            throw new ProjectBuilderException("Failed to create project directory structure under " + projectRoot, e);
        }
    }
}
