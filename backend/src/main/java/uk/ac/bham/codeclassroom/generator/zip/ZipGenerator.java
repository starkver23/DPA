package uk.ac.bham.codeclassroom.generator.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

/**
 * Service responsible for compressing a standalone Maven-based Spring Boot project into a ZIP archive.
 */
public class ZipGenerator {

    /**
     * Compresses a complete generated project directory into a ZIP archive.
     *
     * @param projectDirectory root of the generated Spring Boot project
     * @param outputDirectory  folder where the ZIP file will be stored
     * @return path to the produced ZIP file
     */
    public Path generateZip(Path projectDirectory, Path outputDirectory) {
        if (projectDirectory == null) {
            throw new ZipGenerationException("Project directory cannot be null");
        }
        if (outputDirectory == null) {
            throw new ZipGenerationException("Output directory cannot be null");
        }

        // Validate project directory existence
        if (!Files.exists(projectDirectory)) {
            throw new ZipGenerationException("Project directory does not exist: " + projectDirectory);
        }
        if (!Files.isDirectory(projectDirectory)) {
            throw new ZipGenerationException("Project directory is not a valid directory: " + projectDirectory);
        }

        // Validate Maven project structure (presence of pom.xml)
        if (!Files.exists(projectDirectory.resolve("pom.xml"))) {
            throw new ZipGenerationException("Project directory is not a valid Maven project (pom.xml is missing): " + projectDirectory);
        }

        // Ensure output directory exists or can be created
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new ZipGenerationException("Output directory cannot be created: " + outputDirectory, e);
        }

        String zipFilename = projectDirectory.getFileName().toString() + ".zip";
        Path zipFilePath = outputDirectory.resolve(zipFilename);

        Path rootParent = projectDirectory.toAbsolutePath().getParent();
        if (rootParent == null) {
            rootParent = projectDirectory;
        }

        final Path finalParent = rootParent;

        try (OutputStream fos = Files.newOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos);
             Stream<Path> walk = Files.walk(projectDirectory)) {

            walk.forEach(path -> {
                String entryName = finalParent.relativize(path.toAbsolutePath()).toString().replace('\\', '/');

                if (entryName.isEmpty()) {
                    return;
                }

                try {
                    if (Files.isDirectory(path)) {
                        if (!entryName.endsWith("/")) {
                            entryName = entryName + "/";
                        }
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.closeEntry();
                    } else {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    throw new ZipGenerationException("Failed to add zip entry: " + entryName, e);
                }
            });

        } catch (IOException e) {
            throw new ZipGenerationException("ZIP creation failure on " + zipFilePath, e);
        }

        return zipFilePath;
    }
}
