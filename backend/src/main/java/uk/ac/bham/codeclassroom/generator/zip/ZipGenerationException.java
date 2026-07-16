package uk.ac.bham.codeclassroom.generator.zip;

/**
 * Custom exception representing failures during standalone project ZIP archiving.
 */
public class ZipGenerationException extends RuntimeException {

    public ZipGenerationException(String message) {
        super(message);
    }

    public ZipGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
