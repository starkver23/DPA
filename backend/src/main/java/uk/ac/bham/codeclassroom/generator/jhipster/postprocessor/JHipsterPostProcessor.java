package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Main post-processor that transforms standard JHipster generated source code to include inheritance.
 */
public class JHipsterPostProcessor {

    private final InheritanceInjector injector;
    private final RepositoryTransformer repositoryTransformer;
    private final ServiceTransformer serviceTransformer;
    private final ControllerTransformer controllerTransformer;

    /**
     * Default constructor.
     */
    public JHipsterPostProcessor() {
        this.injector = new InheritanceInjector();
        this.repositoryTransformer = new RepositoryTransformer();
        this.serviceTransformer = new ServiceTransformer();
        this.controllerTransformer = new ControllerTransformer();
    }

    /**
     * Transforms a generated JHipster project path in-place using compiler inheritance metadata.
     *
     * @param generatedProject the path to the JHipster generated project root
     * @param document         the ExtendedJDLDocument containing inheritance metadata
     * @return the transformed JHipster project path
     * @throws TransformationException if any file or configuration is missing or invalid
     */
    public Path transform(Path generatedProject, ExtendedJDLDocument document) {
        if (generatedProject == null) {
            throw new TransformationException("Generated project path cannot be null");
        }
        if (document == null) {
            throw new TransformationException("ExtendedJDLDocument cannot be null");
        }

        if (!Files.exists(generatedProject)) {
            throw new TransformationException("Expected JHipster generated project directory does not exist: " + generatedProject);
        }

        long startTime = System.currentTimeMillis();

        // 1. Initialize Transformation Context
        TransformationContext context = new TransformationContext(
            generatedProject,
            document,
            new HashMap<>(),
            new HashMap<>()
        );

        List<Path> transformedFiles = new ArrayList<>();

        // 2. Perform transformations
        try {
            Path javaSourceDir = generatedProject.resolve("src/main/java");
            if (Files.exists(javaSourceDir)) {
                System.out.println("Debugging [PostProcessor]: Listing all .java files under " + javaSourceDir);
                boolean studentFound = false;
                try (java.util.stream.Stream<Path> paths = Files.walk(javaSourceDir)) {
                    List<Path> javaFiles = paths.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .collect(java.util.stream.Collectors.toList());
                    for (Path p : javaFiles) {
                        System.out.println("Generated file: " + javaSourceDir.relativize(p));
                        if (p.getFileName().toString().equals("Student.java")) {
                            studentFound = true;
                        }
                    }
                }
                if (!studentFound) {
                    System.out.println("Debugging [PostProcessor]: Student.java NOT FOUND. Logging package structure:");
                    try (java.util.stream.Stream<Path> paths = Files.walk(javaSourceDir)) {
                        paths.filter(Files::isDirectory).forEach(d -> System.out.println("Directory: " + javaSourceDir.relativize(d)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        injector.inject(context, transformedFiles);
        repositoryTransformer.transform(context, transformedFiles);
        serviceTransformer.transform(context, transformedFiles);
        controllerTransformer.transform(context, transformedFiles);

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("CodeClassroom Inheritance Post-Processor completed in " + duration + " ms.");
        System.out.println("Transformed " + transformedFiles.size() + " files.");

        return generatedProject;
    }
}
