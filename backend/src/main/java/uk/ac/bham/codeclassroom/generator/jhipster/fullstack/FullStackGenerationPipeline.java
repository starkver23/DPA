package uk.ac.bham.codeclassroom.generator.jhipster.fullstack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jhipster.pipeline.JHipsterGenerationPipeline;
import uk.ac.bham.codeclassroom.generator.jhipster.postprocessor.JHipsterPostProcessor;

import java.nio.file.Path;

/**
 * Coordinates and orchestrates the complete Full Stack JHipster Application Generation pipeline.
 * Performs environment validation, standard JDL generation, inheritance post-processing, and final verification.
 */
public class FullStackGenerationPipeline {

    private static final Logger log = LoggerFactory.getLogger(FullStackGenerationPipeline.class);

    private final JHipsterGenerationPipeline standardPipeline;
    private final JHipsterPostProcessor postProcessor;
    private final GeneratedProjectVerifier verifier;

    /**
     * Default constructor.
     */
    public FullStackGenerationPipeline() {
        this.standardPipeline = new JHipsterGenerationPipeline();
        this.postProcessor = new JHipsterPostProcessor();
        this.verifier = new GeneratedProjectVerifier();
    }

    /**
     * Constructor allowing injection of pipeline sub-stages for testing or customizing.
     *
     * @param standardPipeline the JHipster JDL generation pipeline
     * @param postProcessor    the inheritance post-processor
     * @param verifier         the generated project verifier
     */
    public FullStackGenerationPipeline(
        JHipsterGenerationPipeline standardPipeline,
        JHipsterPostProcessor postProcessor,
        GeneratedProjectVerifier verifier
    ) {
        this.standardPipeline = standardPipeline;
        this.postProcessor = postProcessor;
        this.verifier = verifier;
    }

    /**
     * Sets the local JHipster fork path and propagates it to the standard pipeline.
     *
     * @param jhipsterForkPath the path to the JHipster fork
     */
    public void setJHipsterForkPath(String jhipsterForkPath) {
        if (this.standardPipeline != null) {
            this.standardPipeline.setJHipsterForkPath(jhipsterForkPath);
        }
    }

    /**
     * Generates a fully transformed, JHipster full-stack inheritance-aware application.
     *
     * @param document        the ExtendedJDLDocument containing inheritance metadata and configs
     * @param outputDirectory the final target directory for the generated application
     * @return the path to the verified generated project directory
     * @throws FullStackGenerationException if environment checks, generation, post-processing, or verification fails
     */
    public Path generate(ExtendedJDLDocument document, Path outputDirectory) {
        if (document == null) {
            throw new FullStackGenerationException("ExtendedJDLDocument cannot be null");
        }
        if (outputDirectory == null) {
            throw new FullStackGenerationException("Output directory cannot be null");
        }

        try {
            // 1. Invoke standard JDL generation pipeline
            standardPipeline.generate(document, outputDirectory);

            try {
                Path javaSourceDir = outputDirectory.resolve("src/main/java");
                if (java.nio.file.Files.exists(javaSourceDir)) {
                    log.info("Debugging [Pipeline]: Listing all .java files under " + javaSourceDir);
                    boolean studentFound = false;
                    try (java.util.stream.Stream<Path> paths = java.nio.file.Files.walk(javaSourceDir)) {
                        java.util.List<Path> javaFiles = paths.filter(java.nio.file.Files::isRegularFile)
                                .filter(p -> p.toString().endsWith(".java"))
                                .collect(java.util.stream.Collectors.toList());
                        for (Path p : javaFiles) {
                            log.info("Generated file: {}", javaSourceDir.relativize(p));
                            if (p.getFileName().toString().equals("Student.java")) {
                                studentFound = true;
                            }
                        }
                    }
                    if (!studentFound) {
                        log.error("Debugging [Pipeline]: Student.java NOT FOUND. Logging package structure:");
                        try (java.util.stream.Stream<Path> paths = java.nio.file.Files.walk(javaSourceDir)) {
                            paths.filter(java.nio.file.Files::isDirectory).forEach(d -> log.error("Directory: {}", javaSourceDir.relativize(d)));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Debugging [Pipeline] error", e);
            }

            // 2. Invoke JHipster Inheritance Post-Processor
            postProcessor.transform(outputDirectory, document);

            // 3. Verify complete JHipster Full-Stack project structure & annotations
            verifier.verify(outputDirectory, document);

            return outputDirectory;

        } catch (FullStackGenerationException e) {
            log.error("Full-stack JHipster generation failed with specific pipeline exception", e);
            throw e;
        } catch (Exception e) {
            log.error("Full-stack JHipster generation failed with unexpected exception", e);
            throw new FullStackGenerationException("Full Stack JHipster generation pipeline failed", e);
        }
    }
}
