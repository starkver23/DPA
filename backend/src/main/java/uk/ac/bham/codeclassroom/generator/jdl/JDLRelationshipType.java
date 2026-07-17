package uk.ac.bham.codeclassroom.generator.jdl;

/**
 * Supported relationship types in CodeClassroom JDL.
 */
public enum JDLRelationshipType {
    /**
     * One-to-one relationship.
     */
    OneToOne,

    /**
     * One-to-many relationship.
     */
    OneToMany,

    /**
     * Many-to-one relationship.
     */
    ManyToOne,

    /**
     * Many-to-many relationship.
     */
    ManyToMany
}
