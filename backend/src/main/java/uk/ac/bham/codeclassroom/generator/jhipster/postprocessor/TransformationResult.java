package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import java.nio.file.Path;
import java.util.List;

/**
 * Holds results from a post-processing transformation.
 *
 * @param transformedFiles     the list of file paths that were modified or created
 * @param warnings             warnings encountered during transformation
 * @param executionTimeMs      duration of the transformation in milliseconds
 * @param transformationStatus status of the transformation
 */
public record TransformationResult(
    List<Path> transformedFiles,
    List<String> warnings,
    long executionTimeMs,
    String transformationStatus
) {}
