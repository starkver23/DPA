package uk.ac.bham.codeclassroom.generator.jhipster.fullstack;

import java.nio.file.Path;
import java.util.List;

/**
 * Result representing the output of full-stack JHipster application generation.
 *
 * @param projectPath     the generated project directory path
 * @param status          execution status
 * @param warnings        warnings captured during verification
 * @param executionTimeMs pipeline execution time in milliseconds
 */
public record FullStackGenerationResult(
    Path projectPath,
    String status,
    List<String> warnings,
    long executionTimeMs
) {}
