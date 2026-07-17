package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

import java.nio.file.Path;
import java.util.List;

/**
 * Result representing the output of the JHipster generation process.
 *
 * @param projectPath     the generated project directory path
 * @param executionStatus whether execution succeeded or failed
 * @param executionLogs   standard stdout and stderr logs from JHipster execution
 * @param warnings        warnings captured during the process
 * @param executionTimeMs execution duration in milliseconds
 */
public record JHipsterGenerationResult(
    Path projectPath,
    String executionStatus,
    String executionLogs,
    List<String> warnings,
    long executionTimeMs
) {}
