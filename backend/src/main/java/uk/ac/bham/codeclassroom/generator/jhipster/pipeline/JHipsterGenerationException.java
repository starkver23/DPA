package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

/**
 * Structured exception thrown for any JHipster generation pipeline errors.
 */
public class JHipsterGenerationException extends RuntimeException {

    /**
     * Constructs a JHipsterGenerationException with a detailed message.
     *
     * @param message the error message
     */
    public JHipsterGenerationException(String message) {
        super(message);
    }

    /**
     * Constructs a JHipsterGenerationException with a detailed message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public JHipsterGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
