package uk.ac.bham.codeclassroom.generator.jdl;

import java.util.List;

/**
 * Processor for separating standard JDL information from CodeClassroom custom inheritance metadata.
 */
public class ExtendedJDLProcessor {

    /**
     * Separates standard JDL components from CodeClassroom custom inheritance metadata.
     *
     * @param document the Extended JDL document to process
     * @return a SeparatedJDLResult containing the separate standard JDL document and inheritance declarations
     */
    public SeparatedJDLResult process(ExtendedJDLDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("ExtendedJDLDocument cannot be null");
        }

        // Standard JDL has all fields except the custom inheritance declarations
        ExtendedJDLDocument standardJDL = new ExtendedJDLDocument(
            document.entities(),
            document.relationships(),
            List.of(), // standard JDL doesn't have custom inheritance declarations
            document.configuration()
        );

        return new SeparatedJDLResult(
            standardJDL,
            document.inheritanceDeclarations()
        );
    }
}
