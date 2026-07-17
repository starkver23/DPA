package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import uk.ac.bham.codeclassroom.generator.jdl.JDLInheritance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Post-processor stage to update JHipster domain entity classes with proper inheritance.
 */
public class EntityTransformer {

    /**
     * Applies Java extends and JPA inheritance annotations to JHipster domain entity files.
     *
     * @param context      the transformation context
     * @param changedFiles the list tracking all modified files
     * @throws TransformationException if any file read or write fails
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        Path domainPath = context.generatedProjectPath().resolve("src/main/java")
            .resolve(context.extendedJDLDocument().configuration().basePackage().replace('.', '/'))
            .resolve("domain");

        if (!Files.exists(domainPath)) {
            return;
        }

        // Apply extends clause to children
        for (JDLInheritance inh : context.extendedJDLDocument().inheritanceDeclarations()) {
            Path childFile = domainPath.resolve(inh.childEntity() + ".java");
            if (Files.exists(childFile)) {
                try {
                    String content = Files.readString(childFile);

                    // Replace "public class Child" with "public class Child extends Parent"
                    String targetClassDecl = "public class " + inh.childEntity();
                    if (content.contains(targetClassDecl) && !content.contains(targetClassDecl + " extends")) {
                        content = content.replace(targetClassDecl, targetClassDecl + " extends " + inh.parentEntity());
                    }

                    // Cleanly remove child id field and annotations/getters/setters to support proper JPA inheritance semantics
                    content = stripIdField(content);

                    // Cleanly remove Hibernate second-level cache annotations from child entities in inheritance hierarchies
                    content = stripCacheAnnotation(content);

                    Files.writeString(childFile, content);
                    changedFiles.add(childFile);
                } catch (IOException e) {
                    throw new TransformationException("Failed to transform child entity: " + inh.childEntity(), e);
                }
            } else {
                throw new TransformationException("Missing expected entity file: " + childFile);
            }
        }

        // Apply Inheritance strategy annotation to root entities
        for (JDLInheritance inh : context.extendedJDLDocument().inheritanceDeclarations()) {
            Path parentFile = domainPath.resolve(inh.parentEntity() + ".java");
            if (Files.exists(parentFile)) {
                try {
                    String content = Files.readString(parentFile);
                    String targetClassDecl = "public class " + inh.parentEntity();

                    // Root is an entity that does not extend anything (is not a child in any inheritance declaration)
                    boolean parentIsChild = context.extendedJDLDocument().inheritanceDeclarations().stream()
                        .anyMatch(i -> i.childEntity().equals(inh.parentEntity()));

                    if (!parentIsChild && content.contains(targetClassDecl) && !content.contains("@Inheritance")) {
                        // Insert imports
                        if (!content.contains("import jakarta.persistence.Inheritance;")) {
                            content = content.replace("import jakarta.persistence.*;",
                                "import jakarta.persistence.*;\nimport jakarta.persistence.Inheritance;\nimport jakarta.persistence.InheritanceType;");
                        }

                        // Prep @Inheritance annotation
                        content = content.replace(targetClassDecl, "@Inheritance(strategy = InheritanceType.JOINED)\n" + targetClassDecl);
                        Files.writeString(parentFile, content);
                        changedFiles.add(parentFile);
                    }
                } catch (IOException e) {
                    throw new TransformationException("Failed to transform parent entity: " + inh.parentEntity(), e);
                }
            }
        }
    }

    /**
     * Programmatically removes the id field, associated generator/JPA annotations,
     * and ID getters/setters/builders from child entity files.
     *
     * @param content the original class source code
     * @return the modified class source code without duplicate ID field declarations
     */
    private String stripIdField(String content) {
        if (content == null) {
            return null;
        }

        // 1. Remove @Id annotation and any subsequent generated value / sequence / column annotations up to 'private Long id;'
        // Handles sequence generators, columns, non-greedy, multi-line DOTALL mode
        content = content.replaceAll("(?s)@Id\\s+(@[^\\n]+\\s+)*private\\s+Long\\s+id\\s*;", "");

        // Also handle the simple/stub case if @Id is not present
        content = content.replaceAll("private\\s+Long\\s+id\\s*;", "");

        // 2. Remove getters, setters, and fluid builders of id
        content = content.replaceAll("(?s)public\\s+Long\\s+getId\\(\\)\\s*\\{[^}]*\\}", "");
        content = content.replaceAll("(?s)public\\s+void\\s+setId\\(Long\\s+id\\)\\s*\\{[^}]*\\}", "");
        content = content.replaceAll("(?s)public\\s+\\w+\\s+id\\(Long\\s+id\\)\\s*\\{[^}]*\\}", "");

        return content;
    }

    /**
     * Programmatically removes @Cache annotations and unused Hibernate cache imports
     * from child entity source files.
     *
     * @param content the original class source code
     * @return the modified class source code without child @Cache annotations
     */
    private String stripCacheAnnotation(String content) {
        if (content == null) {
            return null;
        }

        // Remove the @Cache annotation completely (including its parenthesis arguments)
        content = content.replaceAll("@Cache\\s*\\([^)]+\\)", "");

        // Remove Cache imports unconditionally since subclasses do not participate in second-level caching
        content = content.replace("import org.hibernate.annotations.Cache;", "");
        content = content.replace("import org.hibernate.annotations.CacheConcurrencyStrategy;", "");

        return content;
    }
}
