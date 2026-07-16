package uk.ac.bham.codeclassroom.generator.api;

import org.springframework.stereotype.Service;
import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.engine.CodeGenerationEngine;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedProject;
import uk.ac.bham.codeclassroom.generator.inheritance.InheritanceResolver;
import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedInheritanceModel;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.project.ProjectMetadata;
import uk.ac.bham.codeclassroom.generator.project.SpringBootProjectBuilder;
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

    private final SemanticValidator validator = new SemanticValidator();
    private final InheritanceResolver resolver = new InheritanceResolver();
    private final CodeGenerationEngine engine = new CodeGenerationEngine();
    private final SpringBootProjectBuilder projectBuilder = new SpringBootProjectBuilder();
    private final ZipGenerator zipGenerator = new ZipGenerator();

    /**
     * Orchestrates the compiler pipeline: CDL -> AST -> Semantic Checks -> Mustache rendering -> Files -> ZIP.
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

            // 5. Resolve entity inheritance hierarchy
            ResolvedInheritanceModel inheritanceModel = resolver.resolve(cu);

            // 6. Generate layered Spring Boot source files in memory
            GeneratedProject generatedProject = engine.generate(inheritanceModel);

            // 7. Write complete Spring Boot Maven structure on disk
            ProjectMetadata metadata = new ProjectMetadata(
                    "generated-app",
                    "uk.ac.bham.codeclassroom",
                    "generated-app",
                    "uk.ac.bham.codeclassroom",
                    "21",
                    "3.5.0"
            );
            Path projectRoot = projectBuilder.build(generatedProject, metadata, projectBuildDir);

            // 8. Compress project directory into standard standalone ZIP
            Path zipFilePath = zipGenerator.generateZip(projectRoot, zipOutputDir);

            // 9. Immediately prune the raw temporary folder to save disk space
            deleteRecursively(projectBuildDir);

            return zipFilePath;
        } catch (RuntimeException e) {
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
            throw e;
        } catch (Exception e) {
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
