package uk.ac.bham.codeclassroom.generator.jhipster.fullstack;

import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import java.nio.file.Path;
import java.util.Map;

/**
 * Context encapsulating resources for full-stack generation.
 *
 * @param outputDirectory   the final target output directory path
 * @param extendedJDL       the ExtendedJDLDocument containing inheritance metadata
 * @param generationOptions optional generation parameters
 */
public record FullStackGenerationContext(
    Path outputDirectory,
    ExtendedJDLDocument extendedJDL,
    Map<String, String> generationOptions
) {}
