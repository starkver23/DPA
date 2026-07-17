package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

/**
 * Structured exception thrown for any post-processing errors in CodeClassroom.
 */
public class TransformationException extends RuntimeException {

    /**
     * Constructs a TransformationException with a detailed message.
     *
     * @param message the error message
     */
    public TransformationException(String message) {
        super(message);
    }

    /**
     * Constructs a TransformationException with a detailed message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
