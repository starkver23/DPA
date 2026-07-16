package uk.ac.bham.codeclassroom.generator.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles the generation of resource folders and files in the standalone project.
 */
public class ResourceGenerator {

    /**
     * Generates resources such as application.properties and directories for static/templates files.
     *
     * @param resourcesDir the resources directory path
     * @param metadata     the project metadata configurations
     */
    public static void generate(Path resourcesDir, ProjectMetadata metadata) {
        try {
            // Ensure static/ and templates/ resource subdirectories exist
            Files.createDirectories(resourcesDir.resolve("static"));
            Files.createDirectories(resourcesDir.resolve("templates"));

            // Generate application.properties content and write it
            String propertiesContent = ApplicationPropertiesGenerator.generate(metadata);
            Files.writeString(resourcesDir.resolve("application.properties"), propertiesContent);
        } catch (IOException e) {
            throw new ProjectBuilderException("Failed to generate resource files under " + resourcesDir, e);
        }
    }
}
