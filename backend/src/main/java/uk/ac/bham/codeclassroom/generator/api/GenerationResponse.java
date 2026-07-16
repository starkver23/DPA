package uk.ac.bham.codeclassroom.generator.api;

/**
 * Representation of a code generation response (metadata of the generated ZIP).
 *
 * @param message     success description
 * @param filename    the generated ZIP archive name
 * @param sizeInBytes size of the archive
 */
public record GenerationResponse(
    String message,
    String filename,
    long sizeInBytes
) {}
