package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import java.nio.file.Path;
import java.util.Map;

/**
 * Stores the context for transforming a JHipster project.
 *
 * @param generatedProjectPath  the generated JHipster project root directory path
 * @param extendedJDLDocument   the compilation-stage ExtendedJDLDocument containing inheritance metadata
 * @param entityMetadata        additional key-value metadata for entity processing
 * @param transformationOptions additional options for post-processing
 */
public record TransformationContext(
    Path generatedProjectPath,
    ExtendedJDLDocument extendedJDLDocument,
    Map<String, String> entityMetadata,
    Map<String, String> transformationOptions
) {}
