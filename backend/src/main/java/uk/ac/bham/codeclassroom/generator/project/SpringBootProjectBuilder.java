package uk.ac.bham.codeclassroom.generator.project;

import uk.ac.bham.codeclassroom.generator.engine.GeneratedFile;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Orchestrates the conversion of GeneratedProject to a runnable Maven-based Spring Boot project.
 */
public class SpringBootProjectBuilder {

    /**
     * Builds and writes a Spring Boot project onto disk.
     *
     * @param generatedProject the collection of in-memory generated files
     * @param metadata         project metadata configuration
     * @param outputDirectory  path where the project directory will be created
     * @return the resolved project root directory path
     */
    public Path build(
            GeneratedProject generatedProject,
            ProjectMetadata metadata,
            Path outputDirectory
    ) {
        if (generatedProject == null) {
            throw new ProjectBuilderException("Generated project cannot be null");
        }
        if (metadata == null) {
            throw new ProjectBuilderException("Project metadata cannot be null");
        }
        if (outputDirectory == null) {
            throw new ProjectBuilderException("Output directory cannot be null");
        }

        // Initialize project structure and create all required directories
        ProjectStructure structure = new ProjectStructure(outputDirectory, metadata);
        structure.createDirectories();

        // Write pom.xml configuration
        try {
            String pomContent = PomGenerator.generate(metadata);
            Files.writeString(structure.getProjectRoot().resolve("pom.xml"), pomContent);
        } catch (IOException e) {
            throw new ProjectBuilderException("Failed to write pom.xml in " + structure.getProjectRoot(), e);
        }

        // Write properties and resource structures
        ResourceGenerator.generate(structure.getResourcesDir(), metadata);

        // Write main Spring Boot bootstrap application file
        try {
            String mainAppContent = MainApplicationGenerator.generate(metadata);
            Files.writeString(structure.getJavaSourceDir().resolve("GeneratedApplication.java"), mainAppContent);
        } catch (IOException e) {
            throw new ProjectBuilderException("Failed to write GeneratedApplication.java", e);
        }

        // Write the compiler-generated domain files with their packages and imports updated
        writeGeneratedSourceFiles(generatedProject, metadata, structure.getJavaSourceDir());

        return structure.getProjectRoot();
    }

    private void writeGeneratedSourceFiles(GeneratedProject generatedProject, ProjectMetadata metadata, Path javaSourceDir) {
        for (GeneratedFile file : generatedProject.files()) {
            String filename = file.filename();
            String targetSubDir;

            if (filename.endsWith("ServiceImpl.java")) {
                targetSubDir = "service/impl";
            } else if (filename.endsWith("Service.java")) {
                targetSubDir = "service";
            } else if (filename.endsWith("Repository.java")) {
                targetSubDir = "repository";
            } else if (filename.endsWith("Controller.java")) {
                targetSubDir = "controller";
            } else if (filename.endsWith("DTO.java")) {
                targetSubDir = "dto";
            } else if (filename.endsWith("Mapper.java")) {
                targetSubDir = "mapper";
            } else {
                targetSubDir = "model";
            }

            Path destDir = javaSourceDir.resolve(targetSubDir);
            Path destFile = destDir.resolve(filename);

            // Replace occurrences of package prefix 'uk.ac.bham.codeclassroom' with the custom package from metadata
            String adjustedContent = file.content().replace("uk.ac.bham.codeclassroom", metadata.packageName());

            try {
                Files.writeString(destFile, adjustedContent);
            } catch (IOException e) {
                throw new ProjectBuilderException("Failed to write source file " + filename + " to " + destFile, e);
            }
        }
    }
}
