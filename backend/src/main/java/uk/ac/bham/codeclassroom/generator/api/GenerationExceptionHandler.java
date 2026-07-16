package uk.ac.bham.codeclassroom.generator.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.ac.bham.codeclassroom.exception.ErrorResponse;
import uk.ac.bham.codeclassroom.generator.exception.LexerException;
import uk.ac.bham.codeclassroom.generator.exception.ParserException;
import uk.ac.bham.codeclassroom.generator.inheritance.InheritanceResolutionException;
import uk.ac.bham.codeclassroom.generator.project.ProjectBuilderException;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticException;
import uk.ac.bham.codeclassroom.generator.zip.ZipGenerationException;

import java.time.LocalDateTime;

/**
 * Controller advice to catch exceptions thrown during project generation and map them to HTTP responses.
 * Sets highest precedence so it overrides the system-wide global exception handlers for GenerationController.
 */
@RestControllerAdvice(assignableTypes = GenerationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GenerationExceptionHandler {

    @ExceptionHandler(LexerException.class)
    public ResponseEntity<ErrorResponse> handleLexerException(LexerException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                "/api/generate"
        );
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(ParserException.class)
    public ResponseEntity<ErrorResponse> handleParserException(ParserException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                "/api/generate"
        );
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(SemanticException.class)
    public ResponseEntity<ErrorResponse> handleSemanticException(SemanticException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                "/api/generate"
        );
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(InheritanceResolutionException.class)
    public ResponseEntity<ErrorResponse> handleInheritanceResolutionException(InheritanceResolutionException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                "/api/generate"
        );
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorMessage = ex.getBindingResult().getFieldError() != null 
                ? ex.getBindingResult().getFieldError().getDefaultMessage() 
                : "Validation failed";
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                errorMessage,
                "/api/generate"
        );
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(ProjectBuilderException.class)
    public ResponseEntity<ErrorResponse> handleProjectBuilderException(ProjectBuilderException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                "/api/generate"
        );
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(ZipGenerationException.class)
    public ResponseEntity<ErrorResponse> handleZipGenerationException(ZipGenerationException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                "/api/generate"
        );
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                "An unexpected error occurred: " + ex.getMessage(),
                "/api/generate"
        );
        return new ResponseEntity<>(response, status);
    }
}
