package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

/**
 * Result representing system process execution.
 *
 * @param exitCode the execution exit code
 * @param logs     the captured standard output and error output
 */
public record ProcessResult(
    int exitCode,
    String logs
) {}
