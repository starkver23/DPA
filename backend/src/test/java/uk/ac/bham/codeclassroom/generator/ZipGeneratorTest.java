package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.bham.codeclassroom.generator.zip.ZipGenerationException;
import uk.ac.bham.codeclassroom.generator.zip.ZipGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class ZipGeneratorTest {

    private final ZipGenerator zipGenerator = new ZipGenerator();

    @Test
    void testZipGeneratorPipeline(@TempDir Path tempDir) throws IOException {
        // Create mock Spring Boot project directory structure
        Path projectDir = tempDir.resolve("MockProject");
        Files.createDirectories(projectDir);

        // Add pom.xml to pass validation
        Path pomFile = projectDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project></project>");

        // Add application.properties
        Path resourcesDir = projectDir.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);
        Path propertiesFile = resourcesDir.resolve("application.properties");
        Files.writeString(propertiesFile, "spring.application.name=MockProject");

        // Add GeneratedApplication.java
        Path packageDir = projectDir.resolve("src/main/java/com/example/mockproject");
        Files.createDirectories(packageDir);
        Path mainAppFile = packageDir.resolve("GeneratedApplication.java");
        Files.writeString(mainAppFile, "package com.example.mockproject;");

        // Add Entity
        Path modelDir = packageDir.resolve("model");
        Files.createDirectories(modelDir);
        Path studentEntityFile = modelDir.resolve("Student.java");
        Files.writeString(studentEntityFile, "package com.example.mockproject.model;");

        // Add Empty directory to verify empty directories are handled correctly
        Path emptyDir = projectDir.resolve("src/main/resources/static/empty-dir");
        Files.createDirectories(emptyDir);

        // Define output directory
        Path outputDir = tempDir.resolve("output");

        // 1. Run ZIP Generator
        Path zipFileResult = zipGenerator.generateZip(projectDir, outputDir);

        // 2. Verify ZIP file created
        assertNotNull(zipFileResult);
        assertTrue(Files.exists(zipFileResult));

        // 3. Verify ZIP filename is correct
        assertEquals("MockProject.zip", zipFileResult.getFileName().toString());

        // 4. Verify ZIP structure and preservation of folder hierarchy using standard ZipFile
        try (ZipFile zipFile = new ZipFile(zipFileResult.toFile())) {
            // ZIP entries are packaged relative to parent of projectDirectory, so they must start with "MockProject/"
            
            // pom.xml
            ZipEntry pomEntry = zipFile.getEntry("MockProject/pom.xml");
            assertNotNull(pomEntry, "ZIP should contain MockProject/pom.xml");
            assertFalse(pomEntry.isDirectory());

            // application.properties
            ZipEntry propEntry = zipFile.getEntry("MockProject/src/main/resources/application.properties");
            assertNotNull(propEntry, "ZIP should contain MockProject/src/main/resources/application.properties");

            // GeneratedApplication.java
            ZipEntry mainAppEntry = zipFile.getEntry("MockProject/src/main/java/com/example/mockproject/GeneratedApplication.java");
            assertNotNull(mainAppEntry, "ZIP should contain MockProject/src/main/java/com/example/mockproject/GeneratedApplication.java");

            // Student.java
            ZipEntry studentEntry = zipFile.getEntry("MockProject/src/main/java/com/example/mockproject/model/Student.java");
            assertNotNull(studentEntry, "ZIP should contain MockProject/src/main/java/com/example/mockproject/model/Student.java");

            // Empty directory
            ZipEntry emptyDirEntry = zipFile.getEntry("MockProject/src/main/resources/static/empty-dir/");
            assertNotNull(emptyDirEntry, "ZIP should contain empty directory");
            assertTrue(emptyDirEntry.isDirectory());
        }

        // 5. Verify existing ZIP can be overwritten safely (running again does not fail)
        Path overwriteResult = zipGenerator.generateZip(projectDir, outputDir);
        assertNotNull(overwriteResult);
        assertTrue(Files.exists(overwriteResult));
    }

    @Test
    void testInvalidProjectDirectoryThrowsZipGenerationException(@TempDir Path tempDir) throws IOException {
        // Missing pom.xml directory
        Path projectDir = tempDir.resolve("InvalidProject");
        Files.createDirectories(projectDir);

        Path outputDir = tempDir.resolve("output");

        assertThrows(ZipGenerationException.class, () -> zipGenerator.generateZip(projectDir, outputDir));
    }

    @Test
    void testMissingProjectDirectoryThrowsZipGenerationException(@TempDir Path tempDir) {
        Path projectDir = tempDir.resolve("NonExistentProject");
        Path outputDir = tempDir.resolve("output");

        assertThrows(ZipGenerationException.class, () -> zipGenerator.generateZip(projectDir, outputDir));
    }

    @Test
    void testNullInputsThrowZipGenerationException(@TempDir Path tempDir) {
        Path projectDir = tempDir.resolve("MockProject");
        assertThrows(ZipGenerationException.class, () -> zipGenerator.generateZip(null, tempDir));
        assertThrows(ZipGenerationException.class, () -> zipGenerator.generateZip(projectDir, null));
    }
}
