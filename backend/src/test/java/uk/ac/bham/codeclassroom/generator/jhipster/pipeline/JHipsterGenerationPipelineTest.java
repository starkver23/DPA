package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLProcessor;
import uk.ac.bham.codeclassroom.generator.jdl.JDLSerializer;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JHipsterGenerationPipelineTest {

    @TempDir
    Path tempDir;

    private static class StubJHipsterCLIInvoker extends JHipsterCLIInvoker {
        int exitCodeNode = 0;
        int exitCodeNpm = 0;
        int exitCodeJhipster = 0;
        int exitCodeImport = 0;

        @Override
        public ProcessResult executeRawCommand(String command, Path workspace) {
            if (command.contains("node -v")) {
                return new ProcessResult(exitCodeNode, exitCodeNode == 0 ? "v21.5.0" : "node: command not found");
            }
            if (command.contains("npm -v")) {
                return new ProcessResult(exitCodeNpm, exitCodeNpm == 0 ? "10.2.0" : "npm: command not found");
            }
            if (command.contains("jhipster --version")) {
                return new ProcessResult(exitCodeJhipster, exitCodeJhipster == 0 ? "8.2.1" : "jhipster: command not found");
            }
            return new ProcessResult(0, "mock system output");
        }

        @Override
        public ProcessResult invokeJDLImport(Path jdlFilePath, Path workspace) {
            if (exitCodeImport == 0 && workspace != null) {
                try {
                    // Create mock generated directory and file inside the temporary workspace
                    Path mainResources = workspace.resolve("src/main/resources");
                    Files.createDirectories(mainResources);
                    Files.writeString(mainResources.resolve("jhipster-success.txt"), "Standard JHipster Generator Output Content");
                } catch (IOException ignored) {}
            }
            return new ProcessResult(exitCodeImport, exitCodeImport == 0 ? "JDL Import Successful Logs" : "JDL Import Failure Logs");
        }
    }

    @Test
    void testSuccessfulGenerationPipeline() throws IOException {
        StubJHipsterCLIInvoker stubInvoker = new StubJHipsterCLIInvoker();
        JHipsterEnvironmentValidator validator = new JHipsterEnvironmentValidator(stubInvoker);
        JHipsterGenerationPipeline pipeline = new JHipsterGenerationPipeline(
            validator,
            stubInvoker,
            new JDLSerializer(),
            new ExtendedJDLProcessor()
        );

        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("PipelineTestApp");
        ExtendedJDLDocument doc = new ExtendedJDLDocument(List.of(), List.of(), List.of(), config);

        Path finalOutput = tempDir.resolve("target-project");

        Path resultPath = pipeline.generate(doc, finalOutput);

        assertNotNull(resultPath);
        assertEquals(finalOutput.toAbsolutePath(), resultPath.toAbsolutePath());
        assertTrue(Files.exists(finalOutput.resolve("src/main/resources/jhipster-success.txt")));
        assertEquals("Standard JHipster Generator Output Content", Files.readString(finalOutput.resolve("src/main/resources/jhipster-success.txt")));
    }

    @Test
    void testEnvironmentValidationFailures() {
        StubJHipsterCLIInvoker stubInvoker = new StubJHipsterCLIInvoker();
        JHipsterEnvironmentValidator validator = new JHipsterEnvironmentValidator(stubInvoker);

        // 1. Failure on Node
        stubInvoker.exitCodeNode = 127;
        assertThrows(JHipsterGenerationException.class, () -> validator.validate(tempDir));

        // 2. Failure on npm
        stubInvoker.exitCodeNode = 0;
        stubInvoker.exitCodeNpm = 127;
        assertThrows(JHipsterGenerationException.class, () -> validator.validate(tempDir));

        // 3. Failure on JHipster CLI
        stubInvoker.exitCodeNpm = 0;
        stubInvoker.exitCodeJhipster = 127;
        assertThrows(JHipsterGenerationException.class, () -> validator.validate(tempDir));
    }

    @Test
    void testJDLImportFailureThrows() {
        StubJHipsterCLIInvoker stubInvoker = new StubJHipsterCLIInvoker();
        stubInvoker.exitCodeImport = 1; // force CLI generation failure

        JHipsterEnvironmentValidator validator = new JHipsterEnvironmentValidator(stubInvoker);
        JHipsterGenerationPipeline pipeline = new JHipsterGenerationPipeline(
            validator,
            stubInvoker,
            new JDLSerializer(),
            new ExtendedJDLProcessor()
        );

        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("FailureApp");
        ExtendedJDLDocument doc = new ExtendedJDLDocument(List.of(), List.of(), List.of(), config);

        Path finalOutput = tempDir.resolve("target-fail");

        JHipsterGenerationException exception = assertThrows(
            JHipsterGenerationException.class,
            () -> pipeline.generate(doc, finalOutput)
        );

        assertTrue(exception.getMessage().contains("JHipster generation CLI execution failed"));
        assertTrue(exception.getMessage().contains("JDL Import Failure Logs"));
    }

    @Test
    void testInvalidInputsThrow() {
        JHipsterGenerationPipeline pipeline = new JHipsterGenerationPipeline();
        assertThrows(JHipsterGenerationException.class, () -> pipeline.generate(null, tempDir));
        assertThrows(JHipsterGenerationException.class, () -> pipeline.generate(new ExtendedJDLDocument(List.of(), List.of(), List.of(), null), null));
    }
}
