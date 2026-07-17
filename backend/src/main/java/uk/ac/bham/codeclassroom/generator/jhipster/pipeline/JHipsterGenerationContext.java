package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;

import java.nio.file.Path;
import java.util.Map;

/**
 * Context that encapsulates all resources and configurations during JHipster generation.
 *
 * @param configuration      the project configuration
 * @param outputDirectory    the final output directory path
 * @param temporaryWorkspace the temporary workspace directory path
 * @param extendedJDL        the source ExtendedJDLDocument
 * @param generationOptions  optional generation flags and settings
 */
public record JHipsterGenerationContext(
    JHipsterProjectConfiguration configuration,
    Path outputDirectory,
    Path temporaryWorkspace,
    ExtendedJDLDocument extendedJDL,
    Map<String, String> generationOptions
) {}
