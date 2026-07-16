package uk.ac.bham.codeclassroom.generator.project;

/**
 * Custom runtime exception representing errors during Spring Boot project building.
 */
public class ProjectBuilderException extends RuntimeException {
    
    public ProjectBuilderException(String message) {
        super(message);
    }

    public ProjectBuilderException(String message, Throwable cause) {
        super(message, cause);
    }
}
