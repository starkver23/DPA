package uk.ac.bham.codeclassroom.generator.jhipster.fullstack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jdl.JDLInheritance;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;
import uk.ac.bham.codeclassroom.generator.jhipster.pipeline.JHipsterGenerationPipeline;
import uk.ac.bham.codeclassroom.generator.jhipster.postprocessor.JHipsterPostProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FullStackGenerationPipelineTest {

    @TempDir
    Path tempDir;

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

                // 3. Create Liquibase database folder
                Files.createDirectories(outputDirectory.resolve("src/main/resources/config/liquibase"));

                // 4. Create Docker Compose folder
                Files.createDirectories(outputDirectory.resolve("src/main/docker"));

                // 5. Create i18n folder
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

    @Test
    void testSuccessfulFullStackPipeline() {
        StubJHipsterGenerationPipeline stubGenPipeline = new StubJHipsterGenerationPipeline();
        StubJHipsterPostProcessor stubPostProcessor = new StubJHipsterPostProcessor();
        GeneratedProjectVerifier verifier = new GeneratedProjectVerifier();

        FullStackGenerationPipeline pipeline = new FullStackGenerationPipeline(
            stubGenPipeline,
            stubPostProcessor,
            verifier
        );

        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "MyFullStackApp", "com.mycompany.app", "21", "3.2.5", "8.2.1", "postgresql", "jwt", "maven", "react"
        );
        List<JDLInheritance> inheritances = List.of(
            new JDLInheritance("Student", "Person")
        );
        ExtendedJDLDocument doc = new ExtendedJDLDocument(List.of(), List.of(), inheritances, config);

        Path finalOutput = tempDir.resolve("full-stack-project");

        Path result = pipeline.generate(doc, finalOutput);

        assertNotNull(result);
        assertEquals(finalOutput.toAbsolutePath(), result.toAbsolutePath());

        // Verify standard folders are generated and present
        assertTrue(Files.exists(finalOutput.resolve("src/main/webapp/app/entities")));
        assertTrue(Files.exists(finalOutput.resolve("src/main/resources/config/liquibase")));
        assertTrue(Files.exists(finalOutput.resolve("src/main/docker")));
        assertTrue(Files.exists(finalOutput.resolve("src/main/resources/i18n")));

        // Verify that inheritance was injected and verified successfully
        Path packagePath = finalOutput.resolve("src/main/java/com/mycompany/app");
        assertTrue(Files.exists(packagePath.resolve("domain/Person.java")));
        assertTrue(Files.exists(packagePath.resolve("domain/Student.java")));
        assertTrue(Files.exists(packagePath.resolve("service/dto/StudentDTO.java")));
    }

    @Test
    void testVerifierThrowsOnMissingComponents() throws IOException {
        StubJHipsterGenerationPipeline stubGenPipeline = new StubJHipsterGenerationPipeline() {
            @Override
            public Path generate(ExtendedJDLDocument document, Path outputDirectory) {
                super.generate(document, outputDirectory);
                // Intentionally delete a critical full-stack component to trigger verification failure
                try {
                    Files.deleteIfExists(outputDirectory.resolve("src/main/docker"));
                } catch (IOException ignored) {}
                return outputDirectory;
            }
        };

        StubJHipsterPostProcessor stubPostProcessor = new StubJHipsterPostProcessor();
        GeneratedProjectVerifier verifier = new GeneratedProjectVerifier();

        FullStackGenerationPipeline pipeline = new FullStackGenerationPipeline(
            stubGenPipeline,
            stubPostProcessor,
            verifier
        );

        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("FailVerifyApp");
        ExtendedJDLDocument doc = new ExtendedJDLDocument(List.of(), List.of(), List.of(), config);

        Path finalOutput = tempDir.resolve("fail-verify-project");

        FullStackGenerationException exception = assertThrows(
            FullStackGenerationException.class,
            () -> pipeline.generate(doc, finalOutput)
        );

        assertTrue(exception.getMessage().contains("Missing expected full-stack component"));
        assertTrue(exception.getMessage().contains("Docker"));
    }

    @Test
    void testVerifierThrowsOnMissingInheritanceSignatures() {
        StubJHipsterGenerationPipeline stubGenPipeline = new StubJHipsterGenerationPipeline();
        // Post processor stub that does NOT apply inheritance clauses
        StubJHipsterPostProcessor stubPostProcessor = new StubJHipsterPostProcessor() {
            @Override
            public Path transform(Path generatedProject, ExtendedJDLDocument document) {
                // Do not apply extends/annotations
                return generatedProject;
            }
        };
        GeneratedProjectVerifier verifier = new GeneratedProjectVerifier();

        FullStackGenerationPipeline pipeline = new FullStackGenerationPipeline(
            stubGenPipeline,
            stubPostProcessor,
            verifier
        );

        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "FailInheritVerify", "com.fail", "21", "3.2.5", "8.2.1", "postgresql", "jwt", "maven", "react"
        );
        List<JDLInheritance> inheritances = List.of(new JDLInheritance("Student", "Person"));
        ExtendedJDLDocument doc = new ExtendedJDLDocument(List.of(), List.of(), inheritances, config);

        Path finalOutput = tempDir.resolve("fail-inherit-project");

        FullStackGenerationException exception = assertThrows(
            FullStackGenerationException.class,
            () -> pipeline.generate(doc, finalOutput)
        );

        assertTrue(exception.getMessage().contains("is missing inheritance extends signature") || 
                   exception.getMessage().contains("is missing @Inheritance"));
    }

    @Test
    void testInvalidInputsThrow() {
        FullStackGenerationPipeline pipeline = new FullStackGenerationPipeline();
        assertThrows(FullStackGenerationException.class, () -> pipeline.generate(null, tempDir));
        assertThrows(FullStackGenerationException.class, () -> pipeline.generate(new ExtendedJDLDocument(List.of(), List.of(), List.of(), null), null));
    }
}
