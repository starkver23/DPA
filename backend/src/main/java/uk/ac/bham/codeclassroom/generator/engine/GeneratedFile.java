package uk.ac.bham.codeclassroom.generator.engine;

/**
 * Immutable representation of a generated source file in memory.
 *
 * @param filename     the simple name of the file (e.g. "Student.java")
 * @param relativePath the relative path within the project (e.g. "src/main/java/uk/ac/bham/codeclassroom/model/Student.java")
 * @param content      the rendered text content of the file
 */
public record GeneratedFile(
    String filename,
    String relativePath,
    String content
) {}
