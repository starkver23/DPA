package uk.ac.bham.codeclassroom.generator.semantic;

/**
 * Exception thrown when semantic validation of the CDL AST fails.
 */
public class SemanticException extends RuntimeException {
    private final int line;
    private final int column;
    private final String entityName;
    private final String fieldName;

    public SemanticException(String message) {
        this(message, -1, -1, null, null);
    }

    public SemanticException(String message, int line, int column) {
        this(message, line, column, null, null);
    }

    public SemanticException(String message, int line, int column, String entityName, String fieldName) {
        super(message);
        this.line = line;
        this.column = column;
        this.entityName = entityName;
        this.fieldName = fieldName;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
