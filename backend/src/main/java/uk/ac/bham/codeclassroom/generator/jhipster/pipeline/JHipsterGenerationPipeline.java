package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLProcessor;
import uk.ac.bham.codeclassroom.generator.jdl.JDLSerializer;
import uk.ac.bham.codeclassroom.generator.jdl.SeparatedJDLResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Orchestrator class for the real JHipster JDL generation pipeline.
 */
public class JHipsterGenerationPipeline {

    private final JHipsterEnvironmentValidator validator;
    private final JHipsterCLIInvoker invoker;
    private final JDLSerializer serializer;
    private final ExtendedJDLProcessor processor;

    /**
     * Default constructor.
     */
    public JHipsterGenerationPipeline() {
        this.invoker = new JHipsterCLIInvoker();
        this.validator = new JHipsterEnvironmentValidator(invoker);
        this.serializer = new JDLSerializer();
        this.processor = new ExtendedJDLProcessor();
    }

    /**
     * Constructor allowing injection of system helpers.
     *
     * @param validator  the environment validator
     * @param invoker    the CLI invoker
     * @param serializer the JDL serializer
     * @param processor  the JDL pre-processor
     */
    public JHipsterGenerationPipeline(
        JHipsterEnvironmentValidator validator,
        JHipsterCLIInvoker invoker,
        JDLSerializer serializer,
        ExtendedJDLProcessor processor
    ) {
        this.validator = validator;
        this.invoker = invoker;
        this.serializer = serializer;
        this.processor = processor;
    }

    /**
     * Sets the local JHipster fork path and propagates it to the invoker.
     *
     * @param jhipsterForkPath the path to the JHipster fork
     */
    public void setJHipsterForkPath(String jhipsterForkPath) {
        if (this.invoker != null) {
            this.invoker.setJHipsterForkPath(jhipsterForkPath);
        }
    }

    /**
     * Runs the JHipster JDL pipeline, generating a complete application structure.
     *
     * @param document        the ExtendedJDLDocument containing application config and entities
     * @param outputDirectory the final directory where the generated application should be placed
     * @return the Path pointing to the final generated project
     * @throws JHipsterGenerationException if any phase of validation or CLI generation fails
     */
    public Path generate(ExtendedJDLDocument document, Path outputDirectory) {
        if (document == null) {
            throw new JHipsterGenerationException("ExtendedJDLDocument cannot be null");
        }
        if (outputDirectory == null) {
            throw new JHipsterGenerationException("Output directory cannot be null");
        }

        long startTime = System.currentTimeMillis();

        // 1. Validate environment
        validator.validate(outputDirectory);

        Path tempWorkspace = null;
        try {
            // 2. Create a temporary workspace
            tempWorkspace = Files.createTempDirectory("jhipster-gen-");

            // 3. Separate standard JDL from custom CodeClassroom metadata
            SeparatedJDLResult separated = processor.process(document);

            // 4. Build Generation Context
            Map<String, String> options = new HashMap<>();
            options.put("skipGit", "true");
            options.put("skipInstall", "true");
            JHipsterGenerationContext context = new JHipsterGenerationContext(
                document.configuration(),
                outputDirectory,
                tempWorkspace,
                document,
                options
            );

            // 5. Serialize the pure standard-compliant JDL
            String standardJDLText = serializer.serialize(separated.standardJDLDocument());

            // 6. Write JDL file to the temporary workspace
            Path jdlPath = tempWorkspace.resolve("app.jdl");
            Files.writeString(jdlPath, standardJDLText);

            System.out.println("Debugging [JHipsterPipeline]: Generated JDL:");
            System.out.println(standardJDLText);
            
            System.out.println("Debugging [JHipsterPipeline]: Entities in JDL:");
            if (document.entities() != null) {
                for (uk.ac.bham.codeclassroom.generator.jdl.JDLEntity entity : document.entities()) {
                    System.out.println(" - " + entity.name());
                }
            }

            // 7. Invoke JHipster CLI
            ProcessResult result = invoker.invokeJDLImport(jdlPath, tempWorkspace);

            if (result.exitCode() != 0) {
                throw new JHipsterGenerationException("JHipster generation CLI execution failed: " + result.logs());
            }

            // 8. Copy generated application from temporary workspace to final output directory
            copyDirectory(tempWorkspace, outputDirectory);

            return outputDirectory;

        } catch (IOException e) {
            throw new JHipsterGenerationException("I/O error during JHipster pipeline execution", e);
        } finally {
            // Clean up temporary workspace
            if (tempWorkspace != null) {
                deleteDirectorySilently(tempWorkspace);
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(src -> {
                try {
                    Path dest = target.resolve(source.relativize(src));
                    if (Files.isDirectory(src)) {
                        if (!Files.exists(dest)) {
                            Files.createDirectories(dest);
                        }
                    } else {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy files", e);
                }
            });
        }
    }

    private void deleteDirectorySilently(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted((a, b) -> b.compareTo(a)) // delete children before parents
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ignored) {
                        // ignore cleanup exceptions
                    }
                });
        } catch (Exception ignored) {
            // ignore cleanup exceptions
        }
    }
}
