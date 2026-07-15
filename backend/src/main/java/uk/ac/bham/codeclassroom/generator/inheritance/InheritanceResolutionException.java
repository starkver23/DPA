package uk.ac.bham.codeclassroom.generator.inheritance;

/**
 * Exception thrown when inheritance resolution fails due to cyclical definitions or other logical errors.
 */
public class InheritanceResolutionException extends RuntimeException {
    public InheritanceResolutionException(String message) {
        super(message);
    }
}
