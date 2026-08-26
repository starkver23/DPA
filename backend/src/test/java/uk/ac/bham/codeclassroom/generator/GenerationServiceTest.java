package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.Test;
import uk.ac.bham.codeclassroom.generator.api.GenerationService;
import uk.ac.bham.codeclassroom.generator.exception.LexerException;
import uk.ac.bham.codeclassroom.generator.exception.ParserException;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLGenerator;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLProcessor;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterAdapter;
import uk.ac.bham.codeclassroom.generator.jhipster.fullstack.FullStackGenerationPipeline;
import uk.ac.bham.codeclassroom.generator.jhipster.fullstack.GeneratedProjectVerifier;
import uk.ac.bham.codeclassroom.generator.jhipster.pipeline.JHipsterGenerationPipeline;
import uk.ac.bham.codeclassroom.generator.jhipster.postprocessor.JHipsterPostProcessor;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticException;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticValidator;
import uk.ac.bham.codeclassroom.generator.zip.ZipGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class GenerationServiceTest {

    private static class StubJHipsterGenerationPipeline extends JHipsterGenerationPipeline {
        @Override
        public Path generate(ExtendedJDLDocument document, Path outputDirectory) {
            try {
                String basePackage = document.configuration().basePackage();
                Path packagePath = outputDirectory.resolve("src/main/java").resolve(basePackage.replace('.', '/'));

                // 1. Create Spring Boot Backend folders & files
                Files.createDirectories(packagePath.resolve("domain"));
                Files.createDirectories(packagePath.resolve("repository"));
                Files.createDirectories(packagePath.resolve("service/dto"));
                Files.createDirectories(packagePath.resolve("service/mapper"));
                Files.createDirectories(packagePath.resolve("web/rest"));
                Files.createDirectories(packagePath.resolve("config"));

                Files.writeString(packagePath.resolve("domain/Person.java"), "public class Person {}");
                Files.writeString(packagePath.resolve("domain/Student.java"), "public class Student {}");
                Files.writeString(packagePath.resolve("service/dto/StudentDTO.java"), "public class StudentDTO {}");
                Files.writeString(packagePath.resolve("service/mapper/StudentMapper.java"), "public interface StudentMapper {}");

                // 2. Create React Frontend root & entities directory
                Files.createDirectories(outputDirectory.resolve("src/main/webapp/app/entities"));

                // 3. Create JHipster files
                Files.writeString(outputDirectory.resolve("package.json"), "{}");
                Files.writeString(outputDirectory.resolve(".yo-rc.json"), "{}");
                Files.writeString(outputDirectory.resolve("pom.xml"), "<project></project>");

                // 4. Create Liquibase database folder
                Files.createDirectories(outputDirectory.resolve("src/main/resources/config/liquibase"));

                // 5. Create Docker Compose folder
                Files.createDirectories(outputDirectory.resolve("src/main/docker"));

                // 6. Create i18n folder
                Files.createDirectories(outputDirectory.resolve("src/main/resources/i18n"));

            } catch (IOException e) {
                throw new RuntimeException("Stub workspace setup failed", e);
            }
            return outputDirectory;
        }
    }

    private static class StubJHipsterPostProcessor extends JHipsterPostProcessor {
        @Override
        public Path transform(Path generatedProject, ExtendedJDLDocument document) {
            String basePackage = document.configuration().basePackage();
            Path packagePath = generatedProject.resolve("src/main/java").resolve(basePackage.replace('.', '/'));
            try {
                // Apply extends / inheritance signatures as if the post-processor ran successfully
                Files.writeString(packagePath.resolve("domain/Person.java"), "import jakarta.persistence.Inheritance;\n@Inheritance(strategy = InheritanceType.JOINED)\npublic class Person {}");
                Files.writeString(packagePath.resolve("domain/Student.java"), "public class Student extends Person {}");
                Files.writeString(packagePath.resolve("service/dto/StudentDTO.java"), "public class StudentDTO extends PersonDTO {}");
            } catch (IOException e) {
                throw new RuntimeException("Stub transformation failed", e);
            }
            return generatedProject;
        }
    }

    private final StubJHipsterGenerationPipeline stubPipeline = new StubJHipsterGenerationPipeline();
    private final StubJHipsterPostProcessor stubPostProcessor = new StubJHipsterPostProcessor();
    private final FullStackGenerationPipeline fullStackPipeline = new FullStackGenerationPipeline(
        stubPipeline,
        stubPostProcessor,
        new GeneratedProjectVerifier()
    );

    private final GenerationService service = new GenerationService(
        new SemanticValidator(),
        new JHipsterAdapter(),
        new ExtendedJDLGenerator(),
        fullStackPipeline,
        new ZipGenerator()
    );

    @Test
    void testValidCDLGeneratesZip() throws IOException {
        String cdl = """
                entity Person {
                    name String
                }
                entity Student extends Person {
                    age Integer
                }
                """;
        Path zipPath = service.generateStandaloneProject(cdl);
        assertNotNull(zipPath);
        assertTrue(Files.exists(zipPath));
        assertTrue(zipPath.getFileName().toString().endsWith(".zip"));

        // Verify full-stack ZIP contents using standard ZipFile
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            assertNotNull(zipFile.getEntry("generated-app/pom.xml"), "Should contain pom.xml");
            assertNotNull(zipFile.getEntry("generated-app/package.json"), "Should contain package.json");
            assertNotNull(zipFile.getEntry("generated-app/.yo-rc.json"), "Should contain .yo-rc.json");
            assertNotNull(zipFile.getEntry("generated-app/src/main/webapp/app/entities"), "Should contain React entities folder");
            assertNotNull(zipFile.getEntry("generated-app/src/main/docker"), "Should contain Docker configuration folder");
            assertNotNull(zipFile.getEntry("generated-app/src/main/resources/config/liquibase"), "Should contain Liquibase database folder");
            assertNotNull(zipFile.getEntry("generated-app/src/main/java/com/mycompany/codeclassroom/domain/Student.java"), "Should contain Student.java");
            assertTrue(zipFile.stream()
                .noneMatch(entry -> entry.getName().startsWith("generated-app/src/main/java/uk/ac/bham/codeclassroom/")),
                "Generated app should not contain the old uk/ac/bham package folder");
        } finally {
            // Cleanup ZIP and its temporary parent folder
            Files.deleteIfExists(zipPath);
            Path parent = zipPath.getParent();
            if (parent != null) {
                Files.deleteIfExists(parent);
                Path grandParent = parent.getParent();
                if (grandParent != null && grandParent.getFileName().toString().startsWith("codeclassroom-")) {
                    Files.deleteIfExists(grandParent);
                }
            }
        }
    }

    @Test
    void testJavaSourceZipContainsOnlyModelJavaFiles() throws IOException {
        String cdl = """
                abstract entity Person {
                    name String
                }

                interface Payable {
                    calculateSalary() Double
                }

                entity Student extends Person implements Payable {
                    studentNumber String
                    calculateSalary() Double
                }

                entity Course {
                    title String
                }

                relationship ManyToMany {
                    Student{courses} to Course{students}
                }
                """;

        Path zipPath = service.generateJavaSourceZip(cdl);
        assertNotNull(zipPath);
        assertTrue(Files.exists(zipPath));

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            assertNotNull(zipFile.getEntry("main/java/com/mycompany/codeclassroom/model/Person.java"));
            assertNotNull(zipFile.getEntry("main/java/com/mycompany/codeclassroom/model/Payable.java"));
            assertNotNull(zipFile.getEntry("main/java/com/mycompany/codeclassroom/model/Student.java"));
            assertNotNull(zipFile.getEntry("main/java/com/mycompany/codeclassroom/model/Course.java"));
            assertNull(zipFile.getEntry("main/java/uk/ac/bham/codeclassroom/model/Student.java"));
            assertNull(zipFile.getEntry("pom.xml"));

            String studentSource = new String(zipFile.getInputStream(
                zipFile.getEntry("main/java/com/mycompany/codeclassroom/model/Student.java")
            ).readAllBytes());
            assertTrue(studentSource.contains("package com.mycompany.codeclassroom.model;"));
            assertTrue(studentSource.contains("class Student extends Person implements Payable"));
            assertTrue(studentSource.contains("private java.util.List<Course> courses;"));
        } finally {
            Files.deleteIfExists(zipPath);
        }
    }

    @Test
    void testJavaSourceZipAddsMissingInterfaceMethodStubs() throws IOException {
        String cdl = """
                interface Payable {
                    calculateSalary() Double
                }

                entity Teacher implements Payable {
                    staffId String
                }
                """;

        Path zipPath = service.generateJavaSourceZip(cdl);
        assertNotNull(zipPath);
        assertTrue(Files.exists(zipPath));

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            String teacherSource = new String(zipFile.getInputStream(
                zipFile.getEntry("main/java/com/mycompany/codeclassroom/model/Teacher.java")
            ).readAllBytes());
            assertTrue(teacherSource.contains("package com.mycompany.codeclassroom.model;"));
            assertTrue(teacherSource.contains("class Teacher implements Payable"));
            assertTrue(teacherSource.contains("@Override"));
            assertTrue(teacherSource.contains("public Double calculateSalary()"));
        } finally {
            Files.deleteIfExists(zipPath);
        }
    }

    @Test
    void testLexerErrorThrowsException() {
        String invalidCdl = "entity Student { name @@@ }";
        assertThrows(LexerException.class, () -> service.generateStandaloneProject(invalidCdl));
    }

    @Test
    void testParserErrorThrowsException() {
        String invalidCdl = "entity Student name String }";
        assertThrows(ParserException.class, () -> service.generateStandaloneProject(invalidCdl));
    }

    @Test
    void testSemanticErrorThrowsException() {
        String invalidCdl = """
                entity Student {
                    name String
                    name String
                }
                """;
        assertThrows(SemanticException.class, () -> service.generateStandaloneProject(invalidCdl));
    }

    @Test
    void testInheritanceErrorThrowsException() {
        String invalidCdl = """
                entity Student extends Person {}
                """;
        assertThrows(SemanticException.class, () -> service.generateStandaloneProject(invalidCdl));
    }
}
