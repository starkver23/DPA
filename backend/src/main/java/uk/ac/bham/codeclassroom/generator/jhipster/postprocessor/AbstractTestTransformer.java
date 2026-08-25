package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import uk.ac.bham.codeclassroom.generator.jdl.JDLEntity;
import uk.ac.bham.codeclassroom.generator.jdl.JDLInheritance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Post-processor stage to update generated tests so they do not attempt to instantiate
 * abstract classes, substituting them with concrete subclasses where appropriate.
 */
public class AbstractTestTransformer {

    /**
     * Replaces instantiations of abstract classes with concrete subclasses in tests.
     *
     * @param context      the transformation context
     * @param changedFiles the list tracking all modified files
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        Path testPath = context.generatedProjectPath().resolve("src/test/java");
        if (!Files.exists(testPath)) {
            return;
        }

        for (JDLEntity entity : context.extendedJDLDocument().entities()) {
            if (entity.abstractClass()) {
                String abstractEntityName = entity.name();

                // Find a concrete subclass of this abstract entity
                String concreteSubclass = context.extendedJDLDocument().inheritanceDeclarations().stream()
                    .filter(inh -> inh.parentEntity().equals(abstractEntityName))
                    .map(JDLInheritance::childEntity)
                    .filter(child -> {
                        return context.extendedJDLDocument().entities().stream()
                            .anyMatch(e -> e.name().equals(child) && !e.abstractClass());
                    })
                    .findFirst()
                    .orElse(null);

                if (concreteSubclass == null) {
                    // fallback to any child entity if no non-abstract one is declared
                    concreteSubclass = context.extendedJDLDocument().inheritanceDeclarations().stream()
                        .filter(inh -> inh.parentEntity().equals(abstractEntityName))
                        .map(JDLInheritance::childEntity)
                        .findFirst()
                        .orElse(null);
                }

                if (concreteSubclass != null) {
                    final String targetSubclass = concreteSubclass;
                    final String basePkg = context.extendedJDLDocument().configuration().basePackage();
                    final String importTarget = "import " + basePkg + ".domain." + abstractEntityName + ";";
                    final String importSubclass = "import " + basePkg + ".domain." + targetSubclass + ";";

                    try (Stream<Path> paths = Files.walk(testPath)) {
                        paths.filter(Files::isRegularFile)
                             .filter(p -> p.toString().endsWith(".java"))
                             .forEach(file -> {
                                 try {
                                     String content = Files.readString(file);
                                     String target = "new " + abstractEntityName + "()";
                                     boolean modified = false;

                                     if (content.contains(target)) {
                                         // ponytail: replace abstract class instantiation with concrete subclass instantiation
                                         content = content.replace(target, "new " + targetSubclass + "()");
                                         modified = true;
                                     }

                                     if (modified) {
                                         // Add import if it's missing and target import is present
                                         if (content.contains(importTarget) && !content.contains(importSubclass)) {
                                             content = content.replace(importTarget, importTarget + "\nimport " + importSubclass + ";");
                                         }
                                         Files.writeString(file, content);
                                         if (!changedFiles.contains(file)) {
                                             changedFiles.add(file);
                                         }
                                     }
                                 } catch (IOException e) {
                                     throw new TransformationException("Failed to post-process test file: " + file, e);
                                 }
                             });
                    } catch (IOException e) {
                        throw new TransformationException("Failed to scan test files for abstract entity: " + abstractEntityName, e);
                    }
                }
            }
        }
    }
}
