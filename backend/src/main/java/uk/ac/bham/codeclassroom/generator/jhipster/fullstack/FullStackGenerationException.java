package uk.ac.bham.codeclassroom.generator.jhipster.fullstack;

/**
 * Structured exception thrown for any full-stack JHipster generation pipeline errors.
 */
public class FullStackGenerationException extends RuntimeException {

    /**
     * Constructs a FullStackGenerationException with a detailed message.
     *
     * @param message the error message
     */
    public FullStackGenerationException(String message) {
        super(message);
    }

    /**
     * Constructs a FullStackGenerationException with a detailed message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public FullStackGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
