package uk.ac.bham.codeclassroom.generator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticException;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticValidator;
import uk.ac.bham.codeclassroom.generator.token.Token;

class SemanticValidatorTest {

    private final SemanticValidator validator = new SemanticValidator();

    private CompilationUnit parseCDL(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    @Test
    void testValidModel() {
        String source = """
            entity Student {
                email String
                age Integer
                register()
                calculateSalary(Integer hours): BigDecimal
            }
            entity Course {
                title String
            }
            relationship ManyToMany {
                Student{courses} to Course{students}
            }
        """;
        CompilationUnit cu = parseCDL(source);
        assertDoesNotThrow(() -> validator.validate(cu));
    }

    @Test
    void testDuplicateEntityThrows() {
        String source = """
            entity Student {}
            entity Student {}
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Duplicate entity 'Student'"));
    }

    @Test
    void testDuplicateFieldThrows() {
        String source = """
            entity Student {
                email String
                email String
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Duplicate field 'email' in entity 'Student'"));
    }

    @Test
    void testDuplicateMethodThrows() {
        String source = """
            entity Student {
                login()
                login()
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Duplicate method 'login()' in entity 'Student'"));
    }

    @Test
    void testDuplicateMethodWithSameParametersThrows() {
        String source = """
            entity Student {
                calculate(Integer x)
                calculate(Integer y)
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Duplicate method 'calculate(Integer)' in entity 'Student'"));
    }

    @Test
    void testUnknownParentThrows() {
        String source = """
            entity Student extends Person {
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Unknown parent entity 'Person'"));
    }

    @Test
    void testSelfInheritanceThrows() {
        String source = """
            entity Student extends Student {
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Entity 'Student' cannot extend itself"));
    }

    @Test
    void testCircularInheritanceThrows() {
        String source = """
            entity A extends B {}
            entity B extends C {}
            entity C extends A {}
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Circular inheritance detected: A -> B -> C -> A"));
    }

    @Test
    void testUnknownRelationshipSourceThrows() {
        String source = """
            entity Course {}
            relationship OneToOne {
                Student to Course
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Unknown relationship source entity 'Student'"));
    }

    @Test
    void testUnknownRelationshipTargetThrows() {
        String source = """
            entity Student {}
            relationship OneToOne {
                Student to Course
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Unknown relationship target entity 'Course'"));
    }

    @Test
    void testRelationshipSelfReferenceThrows() {
        String source = """
            entity Student {}
            relationship OneToOne {
                Student to Student
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Relationship self-reference is not allowed"));
    }

    @Test
    void testDuplicateRelationshipThrows() {
        String source = """
            entity Student {}
            entity Course {}
            relationship ManyToMany {
                Student to Course
            }
            relationship ManyToMany {
                Student to Course
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Duplicate relationship detected"));
    }

    @Test
    void testUnknownTypeInSimpleFieldThrows() {
        String source = """
            entity Student {
                address Address
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Unknown type 'Address' referenced in field 'address' of entity 'Student'"));
    }

    @Test
    void testUnknownTypeInGenericFieldThrows() {
        String source = """
            entity Student {
                courses List<Course>
            }
        """;
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Unknown type 'Course' referenced in field 'courses' of entity 'Student'"));
    }

    @Test
    void testUnsupportedGenericContainerThrows() {
        String source = """
            entity Student {
                courses CollectionX<Student>
            }
        """;
        // To force checking the container validation
        CompilationUnit cu = parseCDL(source);
        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(cu));
        assertTrue(ex.getMessage().contains("Unsupported generic type container 'CollectionX'"));
    }

    @Test
    void testMultipleEntitiesWithInheritanceAndFields() {
        String source = """
            entity Person {
                name String
            }
            entity Student extends Person {
                courses List<Course>
            }
            entity Course {
                title String
            }
        """;
        CompilationUnit cu = parseCDL(source);
        assertDoesNotThrow(() -> validator.validate(cu));
    }
}
