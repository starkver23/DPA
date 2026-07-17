package uk.ac.bham.codeclassroom.generator.jdl;

import java.util.List;

/**
 * Result of separating standard JDL from custom CodeClassroom metadata.
 *
 * @param standardJDLDocument    an ExtendedJDLDocument containing only standard JDL constructs (no inheritance)
 * @param inheritanceDeclarations the list of CodeClassroom inheritance declarations
 */
public record SeparatedJDLResult(
    ExtendedJDLDocument standardJDLDocument,
    List<JDLInheritance> inheritanceDeclarations
) {}
