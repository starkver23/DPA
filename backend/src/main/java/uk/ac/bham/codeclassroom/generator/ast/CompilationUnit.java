package uk.ac.bham.codeclassroom.generator.ast;

import java.util.List;

/**
 * Root node of the CDL Abstract Syntax Tree (AST), representing an entire CDL file source.
 *
 * @param entities       the list of parsed entities
 * @param relationships  the list of parsed relationships
 */
public record CompilationUnit(
    List<EntityNode> entities,
    List<RelationshipNode> relationships
) {}
