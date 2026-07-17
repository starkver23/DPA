package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import uk.ac.bham.codeclassroom.generator.jdl.JDLInheritance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Post-processor stage to update generated JHipster DTO classes with correct inheritance hierarchies.
 */
public class DTOTransformer {

    /**
     * Applies extends clauses to DTO classes in the project.
     *
     * @param context      the transformation context
     * @param changedFiles the list tracking all modified files
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        Path dtoPath = context.generatedProjectPath().resolve("src/main/java")
            .resolve(context.extendedJDLDocument().configuration().basePackage().replace('.', '/'))
            .resolve("service").resolve("dto");

        if (!Files.exists(dtoPath)) {
            return;
        }

        for (JDLInheritance inh : context.extendedJDLDocument().inheritanceDeclarations()) {
            Path childDtoFile = dtoPath.resolve(inh.childEntity() + "DTO.java");
            if (Files.exists(childDtoFile)) {
                try {
                    String content = Files.readString(childDtoFile);
                    String targetClassDecl = "public class " + inh.childEntity() + "DTO";

                    if (content.contains(targetClassDecl) && !content.contains(targetClassDecl + " extends")) {
                        content = content.replace(targetClassDecl, targetClassDecl + " extends " + inh.parentEntity() + "DTO");
                        Files.writeString(childDtoFile, content);
                        changedFiles.add(childDtoFile);
                    }
                } catch (IOException e) {
                    throw new TransformationException("Failed to transform DTO: " + inh.childEntity() + "DTO", e);
                }
            }
        }
    }
}
