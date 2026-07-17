package uk.ac.bham.codeclassroom.generator.jhipster;

import org.junit.jupiter.api.Test;
import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.token.Token;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JHipsterAdapterTest {

    private final JHipsterAdapter adapter = new JHipsterAdapter();

    private CompilationUnit parseCDL(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    @Test
    void testAdaptSingleEntity() {
        String source = """
            entity Student {
                name String
                age Integer
                register()
                calculateAverage(Double gpa): Double
            }
        """;
        CompilationUnit cu = parseCDL(source);
        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "TestApp", "uk.ac.bham.codeclassroom", "21", "3.2.5", "8.2.1", "postgresql", "jwt", "maven", "react"
        );
        JHipsterProject project = adapter.adapt(cu, config);

        assertNotNull(project);
        assertEquals(1, project.entities().size());

        JHipsterEntity student = project.entities().get(0);
        assertEquals("Student", student.entityName());

        // Verify fields
        assertEquals(2, student.fields().size());
        assertEquals("name", student.fields().get(0).name());
        assertEquals("String", student.fields().get(0).type().toString());
        assertEquals("age", student.fields().get(1).name());
        assertEquals("Integer", student.fields().get(1).type().toString());

        // Verify methods
        assertEquals(2, student.methods().size());
        JHipsterMethod register = student.methods().get(0);
        assertEquals("register", register.name());
        assertTrue(register.parameters().isEmpty());
        assertTrue(register.returnType().isEmpty());

        JHipsterMethod calc = student.methods().get(1);
        assertEquals("calculateAverage", calc.name());
        assertEquals(1, calc.parameters().size());
        assertEquals("gpa", calc.parameters().get(0).name());
        assertEquals("Double", calc.parameters().get(0).type().toString());
        assertTrue(calc.returnType().isPresent());
        assertEquals("Double", calc.returnType().get().toString());

        // Verify configuration
        assertNotNull(project.configuration());
        assertEquals("TestApp", project.configuration().applicationName());
        assertEquals("uk.ac.bham.codeclassroom", project.configuration().basePackage());
        assertEquals("21", project.configuration().javaVersion());
        assertEquals("postgresql", project.configuration().databaseType());
        assertEquals("jwt", project.configuration().authenticationType());
    }

    @Test
    void testAdaptInheritanceTree() {
        String source = """
            entity Person {
                id Long
            }
            entity Student extends Person {
                grade String
            }
            entity Undergrad extends Student {
                major String
            }
        """;
        CompilationUnit cu = parseCDL(source);
        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("InheritanceApp");
        JHipsterProject project = adapter.adapt(cu, config);

        assertNotNull(project);
        assertEquals(3, project.entities().size());

        // Find and verify Person (root)
        JHipsterEntity person = project.entities().stream()
            .filter(e -> e.entityName().equals("Person"))
            .findFirst()
            .orElseThrow();
        JHipsterInheritance personInheritance = person.inheritance();
        assertTrue(personInheritance.isRoot());
        assertTrue(personInheritance.parentEntity().isEmpty());
        assertEquals(0, personInheritance.inheritanceDepth());
        assertTrue(personInheritance.childEntities().contains("Student"));
        assertTrue(personInheritance.requiresInheritanceAnnotations());

        // Find and verify Student (intermediate subclass)
        JHipsterEntity student = project.entities().stream()
            .filter(e -> e.entityName().equals("Student"))
            .findFirst()
            .orElseThrow();
        JHipsterInheritance studentInheritance = student.inheritance();
        assertFalse(studentInheritance.isRoot());
        assertTrue(studentInheritance.parentEntity().isPresent());
        assertEquals("Person", studentInheritance.parentEntity().get());
        assertEquals(1, studentInheritance.inheritanceDepth());
        assertTrue(studentInheritance.childEntities().contains("Undergrad"));
        assertTrue(studentInheritance.requiresInheritanceAnnotations());

        // Find and verify Undergrad (leaf subclass)
        JHipsterEntity undergrad = project.entities().stream()
            .filter(e -> e.entityName().equals("Undergrad"))
            .findFirst()
            .orElseThrow();
        JHipsterInheritance undergradInheritance = undergrad.inheritance();
        assertFalse(undergradInheritance.isRoot());
        assertTrue(undergradInheritance.parentEntity().isPresent());
        assertEquals("Student", undergradInheritance.parentEntity().get());
        assertEquals(2, undergradInheritance.inheritanceDepth());
        assertTrue(undergradInheritance.childEntities().isEmpty());
        assertFalse(undergradInheritance.requiresInheritanceAnnotations());

        // Verify topological order / generation order: parent before child
        assertTrue(personInheritance.generationOrder() < studentInheritance.generationOrder());
        assertTrue(studentInheritance.generationOrder() < undergradInheritance.generationOrder());
    }

    @Test
    void testAdaptRelationships() {
        String source = """
            entity Student {}
            entity Course {}
            entity Address {}
            entity Department {}
            entity Professor {}

            relationship OneToOne {
                Student to Address{student}
            }
            relationship OneToMany {
                Department{professors} to Professor
            }
            relationship ManyToMany {
                Student{courses} to Course{students}
            }
        """;
        CompilationUnit cu = parseCDL(source);
        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("RelApp");
        JHipsterProject project = adapter.adapt(cu, config);

        assertNotNull(project);
        assertNotNull(project.relationships());

        // 1. OneToOne (Student to Address) - bidirectional mapping because Address{student} is specified
        List<JHipsterRelationship> studentRelationships = project.entities().stream()
            .filter(e -> e.entityName().equals("Student"))
            .findFirst()
            .orElseThrow()
            .relationships();

        // Student has: OneToOne to Address
        assertEquals(2, studentRelationships.size()); // OneToOne to Address, ManyToMany to Course
        JHipsterRelationship sToA = studentRelationships.stream()
            .filter(r -> r.targetEntity().equals("Address"))
            .findFirst()
            .orElseThrow();
        assertEquals(JHipsterRelationshipType.OneToOne, sToA.type());
        assertFalse(sToA.sourceProperty().isPresent());
        assertTrue(sToA.targetProperty().isPresent());
        assertEquals("student", sToA.targetProperty().get());

        // Address has: OneToOne to Student (bidirectional)
        List<JHipsterRelationship> addressRelationships = project.entities().stream()
            .filter(e -> e.entityName().equals("Address"))
            .findFirst()
            .orElseThrow()
            .relationships();
        assertEquals(1, addressRelationships.size());
        JHipsterRelationship aToS = addressRelationships.get(0);
        assertEquals(JHipsterRelationshipType.OneToOne, aToS.type());
        assertEquals("Address", aToS.sourceEntity());
        assertTrue(aToS.sourceProperty().isPresent());
        assertEquals("student", aToS.sourceProperty().get());
        assertEquals("Student", aToS.targetEntity());
        assertFalse(aToS.targetProperty().isPresent());

        // 2. OneToMany (Department{professors} to Professor) -> generates OneToMany and ManyToOne
        List<JHipsterRelationship> deptRelationships = project.entities().stream()
            .filter(e -> e.entityName().equals("Department"))
            .findFirst()
            .orElseThrow()
            .relationships();
        assertEquals(1, deptRelationships.size());
        JHipsterRelationship dToP = deptRelationships.get(0);
        assertEquals(JHipsterRelationshipType.OneToMany, dToP.type());
        assertTrue(dToP.sourceProperty().isPresent());
        assertEquals("professors", dToP.sourceProperty().get());

        List<JHipsterRelationship> profRelationships = project.entities().stream()
            .filter(e -> e.entityName().equals("Professor"))
            .findFirst()
            .orElseThrow()
            .relationships();
        assertEquals(1, profRelationships.size());
        JHipsterRelationship pToD = profRelationships.get(0);
        assertEquals(JHipsterRelationshipType.ManyToOne, pToD.type());
        assertEquals("Professor", pToD.sourceEntity());
        assertFalse(pToD.sourceProperty().isPresent());
        assertEquals("Department", pToD.targetEntity());
        assertTrue(pToD.targetProperty().isPresent());
        assertEquals("professors", pToD.targetProperty().get());

        // 3. ManyToMany (Student{courses} to Course{students}) -> generates bidirectional ManyToMany
        JHipsterRelationship sToC = studentRelationships.stream()
            .filter(r -> r.targetEntity().equals("Course"))
            .findFirst()
            .orElseThrow();
        assertEquals(JHipsterRelationshipType.ManyToMany, sToC.type());
        assertTrue(sToC.sourceProperty().isPresent());
        assertEquals("courses", sToC.sourceProperty().get());
        assertTrue(sToC.targetProperty().isPresent());
        assertEquals("students", sToC.targetProperty().get());

        List<JHipsterRelationship> courseRelationships = project.entities().stream()
            .filter(e -> e.entityName().equals("Course"))
            .findFirst()
            .orElseThrow()
            .relationships();
        assertEquals(1, courseRelationships.size());
        JHipsterRelationship cToS = courseRelationships.get(0);
        assertEquals(JHipsterRelationshipType.ManyToMany, cToS.type());
        assertEquals("Course", cToS.sourceEntity());
        assertTrue(cToS.sourceProperty().isPresent());
        assertEquals("students", cToS.sourceProperty().get());
        assertEquals("Student", cToS.targetEntity());
        assertTrue(cToS.targetProperty().isPresent());
        assertEquals("courses", cToS.targetProperty().get());
    }

    @Test
    void testAdaptInvalidInputs() {
        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("App");
        assertThrows(IllegalArgumentException.class, () -> adapter.adapt(null, config));
        assertThrows(IllegalArgumentException.class, () -> adapter.adapt(parseCDL("entity X {}"), null));

        EntityMetadataGenerator entityGenerator = new EntityMetadataGenerator();
        assertThrows(IllegalArgumentException.class, () -> entityGenerator.generate(null, null, null));

        RelationshipMetadataGenerator relationshipGenerator = new RelationshipMetadataGenerator();
        assertThrows(IllegalArgumentException.class, () -> relationshipGenerator.mapDirect(null));

        InheritanceMetadataGenerator inheritanceGenerator = new InheritanceMetadataGenerator();
        assertThrows(IllegalArgumentException.class, () -> inheritanceGenerator.generate(null, null));
    }
}
