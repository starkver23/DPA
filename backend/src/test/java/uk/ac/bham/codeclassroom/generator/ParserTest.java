package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.Test;
import uk.ac.bham.codeclassroom.generator.ast.*;
import uk.ac.bham.codeclassroom.generator.exception.ParserException;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.token.Token;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private CompilationUnit parseCDL(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    @Test
    void testSingleEntity() {
        String source = """
            entity Student {
                email String
                age Integer
            }
        """;
        CompilationUnit cu = parseCDL(source);

        assertEquals(1, cu.entities().size());
        assertEquals(0, cu.relationships().size());

        EntityNode student = cu.entities().get(0);
        assertEquals("Student", student.name());
        assertFalse(student.inheritance().isPresent());
        assertEquals(2, student.fields().size());
        assertEquals(0, student.methods().size());

        FieldNode email = student.fields().get(0);
        assertEquals("email", email.name());
        assertEquals("String", email.type().baseType());
        assertFalse(email.type().genericType().isPresent());

        FieldNode age = student.fields().get(1);
        assertEquals("age", age.name());
        assertEquals("Integer", age.type().baseType());
    }

    @Test
    void testInheritance() {
        String source = """
            entity Student extends Person {
            }
        """;
        CompilationUnit cu = parseCDL(source);

        assertEquals(1, cu.entities().size());
        EntityNode student = cu.entities().get(0);
        assertEquals("Student", student.name());
        assertTrue(student.inheritance().isPresent());
        assertEquals("Person", student.inheritance().get().parentName());
    }

    @Test
    void testGenericFields() {
        String source = """
            entity Professor {
                managedCourses List<Course>
            }
        """;
        CompilationUnit cu = parseCDL(source);

        EntityNode professor = cu.entities().get(0);
        FieldNode field = professor.fields().get(0);
        assertEquals("managedCourses", field.name());
        assertEquals("List", field.type().baseType());
        assertTrue(field.type().genericType().isPresent());
        assertEquals("Course", field.type().genericType().get().baseType());
    }

    @Test
    void testMethods() {
        String source = """
            entity Account {
                login()
                getName(): String
                calculateSalary(Integer hours, BigDecimal rate): BigDecimal
            }
        """;
        CompilationUnit cu = parseCDL(source);

        EntityNode account = cu.entities().get(0);
        assertEquals(3, account.methods().size());

        // login()
        MethodNode login = account.methods().get(0);
        assertEquals("login", login.name());
        assertEquals(0, login.parameters().size());
        assertFalse(login.returnType().isPresent());

        // getName(): String
        MethodNode getName = account.methods().get(1);
        assertEquals("getName", getName.name());
        assertEquals(0, getName.parameters().size());
        assertTrue(getName.returnType().isPresent());
        assertEquals("String", getName.returnType().get().baseType());

        // calculateSalary(Integer hours, BigDecimal rate): BigDecimal
        MethodNode calc = account.methods().get(2);
        assertEquals("calculateSalary", calc.name());
        assertEquals(2, calc.parameters().size());
        
        ParameterNode p1 = calc.parameters().get(0);
        assertEquals("hours", p1.name());
        assertEquals("Integer", p1.type().baseType());

        ParameterNode p2 = calc.parameters().get(1);
        assertEquals("rate", p2.name());
        assertEquals("BigDecimal", p2.type().baseType());

        assertTrue(calc.returnType().isPresent());
        assertEquals("BigDecimal", calc.returnType().get().baseType());
    }

    @Test
    void testRelationships() {
        String source = """
            relationship OneToOne {
                Student to Address
            }
            relationship OneToMany {
                Department{professors} to Professor
            }
            relationship ManyToMany {
                Student{courses} to Course{students}
            }
        """;
        CompilationUnit cu = parseCDL(source);

        assertEquals(0, cu.entities().size());
        assertEquals(3, cu.relationships().size());

        // OneToOne
        RelationshipNode rel1 = cu.relationships().get(0);
        assertEquals(RelationshipType.OneToOne, rel1.type());
        assertEquals("Student", rel1.sourceEntity());
        assertFalse(rel1.sourceProperty().isPresent());
        assertEquals("Address", rel1.targetEntity());
        assertFalse(rel1.targetProperty().isPresent());

        // OneToMany
        RelationshipNode rel2 = cu.relationships().get(1);
        assertEquals(RelationshipType.OneToMany, rel2.type());
        assertEquals("Department", rel2.sourceEntity());
        assertTrue(rel2.sourceProperty().isPresent());
        assertEquals("professors", rel2.sourceProperty().get());
        assertEquals("Professor", rel2.targetEntity());
        assertFalse(rel2.targetProperty().isPresent());

        // ManyToMany
        RelationshipNode rel3 = cu.relationships().get(2);
        assertEquals(RelationshipType.ManyToMany, rel3.type());
        assertEquals("Student", rel3.sourceEntity());
        assertTrue(rel3.sourceProperty().isPresent());
        assertEquals("courses", rel3.sourceProperty().get());
        assertEquals("Course", rel3.targetEntity());
        assertTrue(rel3.targetProperty().isPresent());
        assertEquals("students", rel3.targetProperty().get());
    }

    @Test
    void testMalformedEntityThrows() {
        String source = "entity Student email String }"; // missing opening {
        assertThrows(ParserException.class, () -> parseCDL(source));
    }

    @Test
    void testMalformedRelationshipThrows() {
        String source = "relationship OneToOne { Student Course }"; // missing 'to'
        assertThrows(ParserException.class, () -> parseCDL(source));
    }

    @Test
    void testUnexpectedEOFThrows() {
        String source = "entity Student {"; // unclosed brace
        assertThrows(ParserException.class, () -> parseCDL(source));
    }

    @Test
    void testUnexpectedTopLevelTokenThrows() {
        String source = "String age Integer"; // starts with a type instead of entity/relationship
        assertThrows(ParserException.class, () -> parseCDL(source));
    }
}
