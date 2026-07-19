package uk.ac.bham.codeclassroom.generator.api;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLGenerator;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterAdapter;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProject;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;
import uk.ac.bham.codeclassroom.generator.jhipster.fullstack.FullStackGenerationPipeline;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticValidator;
import uk.ac.bham.codeclassroom.generator.token.Token;
import uk.ac.bham.codeclassroom.generator.zip.ZipGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service that orchestrates the entire compilation and generation pipeline from CDL to a ZIP file.
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    @Value("${codeclassroom.jhipster.fork.path:}")
    private String jhipsterForkPath;

    private final SemanticValidator validator;
    private final JHipsterAdapter adapter;
    private final ExtendedJDLGenerator jdlGenerator;
    private final FullStackGenerationPipeline pipeline;
    private final ZipGenerator zipGenerator;

    @PostConstruct
    public void init() {
        if (this.pipeline != null) {
            log.info("[JHipster] Loaded JHipster local fork path property: '{}'", jhipsterForkPath);
            this.pipeline.setJHipsterForkPath(jhipsterForkPath);
        }
    }

    /**
     * Default constructor.
     */
    public GenerationService() {
        this.validator = new SemanticValidator();
        this.adapter = new JHipsterAdapter();
        this.jdlGenerator = new ExtendedJDLGenerator();
        this.pipeline = new FullStackGenerationPipeline();
        this.zipGenerator = new ZipGenerator();
    }

    /**
     * Constructor allowing injection of generators and pipeline.
     *
     * @param validator    the semantic validator
     * @param adapter      the JHipster adapter
     * @param jdlGenerator the Extended JDL generator
     * @param pipeline     the full stack JHipster generation pipeline
     * @param zipGenerator the ZIP generator
     */
    public GenerationService(
        SemanticValidator validator,
        JHipsterAdapter adapter,
        ExtendedJDLGenerator jdlGenerator,
        FullStackGenerationPipeline pipeline,
        ZipGenerator zipGenerator
    ) {
        this.validator = validator;
        this.adapter = adapter;
        this.jdlGenerator = jdlGenerator;
        this.pipeline = pipeline;
        this.zipGenerator = zipGenerator;
    }

    /**
     * Orchestrates the compiler pipeline: CDL -> AST -> Semantic Checks -> JHipster Adaptation -> Extended JDL -> Full Stack Generation -> Post-Processing -> ZIP.
     *
     * @param cdl source code
     * @return Path to the generated ZIP file
     */
    public Path generateStandaloneProject(String cdl) {
        Path tempDir = null;
        try {
            // 1. Create temporary working directory structure
            tempDir = Files.createTempDirectory("codeclassroom-gen-");
            Path projectBuildDir = tempDir.resolve("build");
            Path zipOutputDir = tempDir.resolve("zip-out");

            Files.createDirectories(projectBuildDir);
            Files.createDirectories(zipOutputDir);

            // 2. Tokenize CDL using Lexer
            Lexer lexer = new Lexer(cdl);
            List<Token> tokens = lexer.tokenize();

            // 3. Parse CDL tokens to AST using Parser
            Parser parser = new Parser(tokens);
            CompilationUnit cu = parser.parse();

            // 4. Validate AST using SemanticValidator
            validator.validate(cu);

            // 5. Adapt CDL AST into a JHipsterProject metadata representation
            // JHipster baseName must match /^[A-Za-z]\w*$/, so generatedApp is used instead of generated-app.
            JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
                "generatedApp",
                "com.mycompany.codeclassroom",
                "21",
                "3.2.5",
                "8.2.1",
                "postgresql",
                "jwt",
                "maven",
                "react"
            );
            JHipsterProject jhipsterProject = adapter.adapt(cu, config);

            // 6. Generate JDL metadata from adapter output
            ExtendedJDLDocument jdlDoc = jdlGenerator.generate(jhipsterProject);

            // 7. Invoke the Full Stack JHipster Generation Pipeline & Inheritance Post-Processor
            Path appOutputDir = projectBuildDir.resolve("generated-app");
            Path projectRoot = pipeline.generate(jdlDoc, appOutputDir);

            // 8. Compress project directory into standard full-stack ZIP
            Path zipFilePath = zipGenerator.generateZip(projectRoot, zipOutputDir);

            // 9. Immediately prune the raw temporary folder to save disk space
            deleteRecursively(projectBuildDir);

            return zipFilePath;
        } catch (RuntimeException e) {
            log.error("Runtime exception during project generation pipeline", e);
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
            throw e;
        } catch (Exception e) {
            log.error("Exception during project generation pipeline", e);
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
            throw new RuntimeException("Project generation failed", e);
        }
    }

    private void deleteRecursively(Path path) {
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {}
                    });
            } catch (IOException ignored) {}
        }
    }
}
