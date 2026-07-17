package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import uk.ac.bham.codeclassroom.generator.jdl.JDLInheritance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Post-processor stage to update generated JHipster MapStruct mapper files.
 */
public class MapperTransformer {

    /**
     * Updates MapStruct mappers to support inheritance-aware mappings.
     *
     * @param context      the transformation context
     * @param changedFiles the list tracking all modified files
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        Path mapperPath = context.generatedProjectPath().resolve("src/main/java")
            .resolve(context.extendedJDLDocument().configuration().basePackage().replace('.', '/'))
            .resolve("service").resolve("mapper");

        if (!Files.exists(mapperPath)) {
            return;
        }

        for (JDLInheritance inh : context.extendedJDLDocument().inheritanceDeclarations()) {
            Path childMapperFile = mapperPath.resolve(inh.childEntity() + "Mapper.java");
            if (Files.exists(childMapperFile)) {
                try {
                    String content = Files.readString(childMapperFile);

                    // MapStruct mappers can be made aware of parent mappings by adding uses = { ParentMapper.class } or custom methods
                    String targetMapperAnnotation = "@Mapper(componentModel = \"spring\"";
                    String updatedAnnotation = "@Mapper(componentModel = \"spring\", uses = { " + inh.parentEntity() + "Mapper.class }";

                    if (content.contains(targetMapperAnnotation) && !content.contains("uses = {")) {
                        content = content.replace(targetMapperAnnotation, updatedAnnotation);
                        Files.writeString(childMapperFile, content);
                        changedFiles.add(childMapperFile);
                    }
                } catch (IOException e) {
                    throw new TransformationException("Failed to transform mapper: " + inh.childEntity() + "Mapper", e);
                }
            }
        }
    }
}
