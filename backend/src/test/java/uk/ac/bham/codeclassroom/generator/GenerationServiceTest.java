package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.Test;
import uk.ac.bham.codeclassroom.generator.api.GenerationService;
import uk.ac.bham.codeclassroom.generator.exception.LexerException;
import uk.ac.bham.codeclassroom.generator.exception.ParserException;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class GenerationServiceTest {

    private final GenerationService service = new GenerationService();

    @Test
    void testValidCDLGeneratesZip() throws IOException {
        String cdl = """
                entity Student {
                    name String
                }
                """;
        Path zipPath = service.generateStandaloneProject(cdl);
        assertNotNull(zipPath);
        assertTrue(Files.exists(zipPath));
        assertTrue(zipPath.getFileName().toString().endsWith(".zip"));

        // Verify ZIP contents using standard ZipFile
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            assertNotNull(zipFile.getEntry("generated-app/pom.xml"));
            assertNotNull(zipFile.getEntry("generated-app/src/main/resources/application.properties"));
            assertNotNull(zipFile.getEntry("generated-app/src/main/java/uk/ac/bham/codeclassroom/GeneratedApplication.java"));
            assertNotNull(zipFile.getEntry("generated-app/src/main/java/uk/ac/bham/codeclassroom/model/Student.java"));
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
