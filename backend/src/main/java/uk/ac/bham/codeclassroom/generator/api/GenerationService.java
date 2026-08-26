package uk.ac.bham.codeclassroom.generator.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.ast.EntityKind;
import uk.ac.bham.codeclassroom.generator.ast.EntityNode;
import uk.ac.bham.codeclassroom.generator.ast.FieldNode;
import uk.ac.bham.codeclassroom.generator.ast.MethodNode;
import uk.ac.bham.codeclassroom.generator.ast.RelationshipNode;
import uk.ac.bham.codeclassroom.generator.ast.RelationshipType;
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
        return generateStandaloneProject(cdl, null);
    }

    /**
     * Orchestrates generation using user-editable project options.
     *
     * @param cdl     source code
     * @param options project configuration overrides
     * @return Path to the generated ZIP file
     */
    public Path generateStandaloneProject(String cdl, ProjectGenerationOptions options) {
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
            JHipsterProjectConfiguration config = options == null
                ? JHipsterProjectConfiguration.createDefault("generatedApp")
                : options.toJHipsterConfiguration();
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

    /**
     * Generates a ZIP containing only Java source files for the parsed CodeClassroom model.
     *
     * @param cdl source code
     * @return Path to the generated Java source ZIP file
     */
    public Path generateJavaSourceZip(String cdl) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("codeclassroom-java-");
            Path sourceRoot = tempDir.resolve("src/main/java/com/mycompany/codeclassroom/model");
            Path zipOutputDir = tempDir.resolve("zip-out");
            Files.createDirectories(sourceRoot);
            Files.createDirectories(zipOutputDir);

            CompilationUnit cu = parseAndValidate(cdl);
            for (EntityNode entity : cu.entities()) {
                Path outputFile = sourceRoot.resolve(entity.name() + ".java");
                Files.writeString(outputFile, renderJavaSource(entity, cu.relationships(), cu.entities()));
            }

            Path zipFile = zipOutputDir.resolve("generated-java-source.zip");
            Path archiveRoot = tempDir.resolve("src");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile));
                 Stream<Path> walk = Files.walk(archiveRoot)) {
                walk.filter(Files::isRegularFile).forEach(path -> {
                    String entryName = archiveRoot.relativize(path).toString().replace('\\', '/');
                    try {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to add Java source to ZIP: " + entryName, e);
                    }
                });
            }

            return zipFile;
        } catch (RuntimeException e) {
            log.error("Runtime exception during Java source generation pipeline", e);
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
            throw e;
        } catch (Exception e) {
            log.error("Exception during Java source generation pipeline", e);
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
            throw new RuntimeException("Java source generation failed", e);
        }
    }

    private CompilationUnit parseAndValidate(String cdl) {
        Lexer lexer = new Lexer(cdl);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        CompilationUnit cu = parser.parse();
        validator.validate(cu);
        return cu;
    }

    private String renderJavaSource(EntityNode entity, List<RelationshipNode> relationships, List<EntityNode> allEntities) {
        StringBuilder source = new StringBuilder();
        source.append("package com.mycompany.codeclassroom.model;\n\n");

        if (entity.kind() == EntityKind.INTERFACE) {
            source.append("public interface ").append(entity.name());
            if (!entity.extendsInterfaces().isEmpty()) {
                source.append(" extends ").append(String.join(", ", entity.extendsInterfaces()));
            }
            source.append(" {\n");
            for (MethodNode method : entity.methods()) {
                source.append("    ").append(methodReturnType(method)).append(" ").append(method.name())
                    .append("(").append(methodParameters(method)).append(");\n");
            }
            source.append("}\n");
            return source.toString();
        }

        source.append("public ");
        if (entity.abstractClass()) {
            source.append("abstract ");
        }
        source.append("class ").append(entity.name());
        entity.inheritance().ifPresent(inheritance -> source.append(" extends ").append(inheritance.parentName()));
        if (!entity.implementsInterfaces().isEmpty()) {
            source.append(" implements ").append(String.join(", ", entity.implementsInterfaces()));
        }
        source.append(" {\n");

        for (FieldNode field : entity.fields()) {
            source.append("    private ").append(field.type()).append(" ").append(field.name()).append(";\n");
        }

        for (RelationshipNode relationship : relationships) {
            appendRelationshipField(source, entity.name(), relationship);
        }

        if (!entity.fields().isEmpty() || relationships.stream().anyMatch(rel -> participates(entity.name(), rel))) {
            source.append("\n");
        }

        Set<String> emittedMethodSignatures = new HashSet<>();
        for (MethodNode method : entity.methods()) {
            appendMethodSource(source, method, false);
            emittedMethodSignatures.add(methodSignature(method));
        }

        if (!entity.abstractClass()) {
            for (MethodNode method : collectRequiredInterfaceMethods(entity, allEntities)) {
                if (emittedMethodSignatures.add(methodSignature(method))) {
                    appendMethodSource(source, method, true);
                }
            }
        }

        source.append("}\n");
        return source.toString();
    }

    private void appendMethodSource(StringBuilder source, MethodNode method, boolean override) {
        if (override) {
            source.append("    @Override\n");
        }
        source.append("    public ").append(methodReturnType(method)).append(" ").append(method.name())
            .append("(").append(methodParameters(method)).append(") {\n");
        source.append("        throw new UnsupportedOperationException(\"Not implemented yet\");\n");
        source.append("    }\n\n");
    }

    private List<MethodNode> collectRequiredInterfaceMethods(EntityNode entity, List<EntityNode> allEntities) {
        Map<String, EntityNode> entityMap = new HashMap<>();
        for (EntityNode candidate : allEntities) {
            entityMap.put(candidate.name(), candidate);
        }

        Set<String> interfaceNames = new HashSet<>();
        EntityNode current = entity;
        while (current != null) {
            for (String interfaceName : current.implementsInterfaces()) {
                collectAllExtendedInterfaces(interfaceName, entityMap, interfaceNames);
            }
            current = current.inheritance().map(inheritance -> entityMap.get(inheritance.parentName())).orElse(null);
        }

        Map<String, MethodNode> methodsBySignature = new LinkedHashMap<>();
        for (String interfaceName : interfaceNames) {
            EntityNode interfaceNode = entityMap.get(interfaceName);
            if (interfaceNode == null) {
                continue;
            }
            for (MethodNode method : interfaceNode.methods()) {
                methodsBySignature.putIfAbsent(methodSignature(method), method);
            }
        }
        return new ArrayList<>(methodsBySignature.values());
    }

    private void collectAllExtendedInterfaces(
        String interfaceName,
        Map<String, EntityNode> entityMap,
        Set<String> interfaceNames
    ) {
        if (!interfaceNames.add(interfaceName)) {
            return;
        }

        EntityNode interfaceNode = entityMap.get(interfaceName);
        if (interfaceNode == null) {
            return;
        }

        for (String parentInterface : interfaceNode.extendsInterfaces()) {
            collectAllExtendedInterfaces(parentInterface, entityMap, interfaceNames);
        }
    }

    private void appendRelationshipField(StringBuilder source, String entityName, RelationshipNode relationship) {
        if (entityName.equals(relationship.sourceEntity())) {
            String property = relationship.sourceProperty().orElse(lowercaseFirst(relationship.targetEntity()));
            String type = relationship.type() == RelationshipType.ManyToMany || relationship.type() == RelationshipType.OneToMany
                ? "java.util.List<" + relationship.targetEntity() + ">"
                : relationship.targetEntity();
            source.append("    private ").append(type).append(" ").append(property).append(";\n");
        }
        if (entityName.equals(relationship.targetEntity()) && relationship.targetProperty().isPresent()) {
            String type = relationship.type() == RelationshipType.ManyToMany
                ? "java.util.List<" + relationship.sourceEntity() + ">"
                : relationship.sourceEntity();
            source.append("    private ").append(type).append(" ").append(relationship.targetProperty().get()).append(";\n");
        }
    }

    private boolean participates(String entityName, RelationshipNode relationship) {
        return entityName.equals(relationship.sourceEntity()) || entityName.equals(relationship.targetEntity());
    }

    private String methodReturnType(MethodNode method) {
        return method.returnType().map(Object::toString).orElse("void");
    }

    private String methodParameters(MethodNode method) {
        return method.parameters().stream()
            .map(parameter -> parameter.type() + " " + parameter.name())
            .collect(java.util.stream.Collectors.joining(", "));
    }

    private String methodSignature(MethodNode method) {
        return method.name() + "(" + method.parameters().stream()
            .map(parameter -> parameter.type().toString())
            .collect(java.util.stream.Collectors.joining(",")) + ")";
    }

    private String lowercaseFirst(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
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
