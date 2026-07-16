package uk.ac.bham.codeclassroom.generator.engine;

import java.util.List;
import java.util.Optional;

/**
 * Encloses the complete collection of generated project files in memory,
 * with utility queries to find individual files by name or relative path.
 *
 * @param files list of all generated files
 */
public record GeneratedProject(
    List<GeneratedFile> files
) {
    /**
     * Finds a generated file by its simple filename (case-sensitive).
     *
     * @param filename the name of the file (e.g. "Student.java")
     * @return an Optional enclosing the file if found
     */
    public Optional<GeneratedFile> findByFilename(String filename) {
        return files.stream()
                .filter(f -> f.filename().equals(filename))
                .findFirst();
    }

    /**
     * Finds a generated file by its relative path.
     *
     * @param path the relative path of the file (e.g. "src/main/java/.../Student.java")
     * @return an Optional enclosing the file if found
     */
    public Optional<GeneratedFile> findByPath(String path) {
        return files.stream()
                .filter(f -> f.relativePath().equals(path))
                .findFirst();
    }
}
