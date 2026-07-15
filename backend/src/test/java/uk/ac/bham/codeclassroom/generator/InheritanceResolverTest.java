package uk.ac.bham.codeclassroom.generator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.inheritance.InheritanceResolutionException;
import uk.ac.bham.codeclassroom.generator.inheritance.InheritanceResolver;
import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedEntity;
import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedInheritanceModel;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.token.Token;

class InheritanceResolverTest {

    private final InheritanceResolver resolver = new InheritanceResolver();

    private CompilationUnit parseCDL(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    @Test
    void testSingleEntity() {
        String source = "entity Student {}";
        CompilationUnit cu = parseCDL(source);
        ResolvedInheritanceModel model = resolver.resolve(cu);

        assertEquals(1, model.orderedEntities().size());
        assertEquals(1, model.rootEntities().size());

        ResolvedEntity student = model.orderedEntities().get(0);
        assertEquals("Student", student.entityName());
        assertFalse(student.hasParent());
        assertTrue(student.isRoot());
        assertTrue(student.isLeaf());
        assertEquals(0, student.depth());
        assertEquals(0, student.generationOrder());
        assertFalse(student.requiresInheritanceAnnotation());
        assertFalse(student.requiresExtends());
    }

    @Test
    void testSimpleInheritance() {
        String source = """
            entity Person {}
            entity Student extends Person {}
        """;
        CompilationUnit cu = parseCDL(source);
        ResolvedInheritanceModel model = resolver.resolve(cu);

        assertEquals(2, model.orderedEntities().size());
        assertEquals(1, model.rootEntities().size());

        ResolvedEntity person = model.lookupByName().get("Person");
        assertNotNull(person);
        assertTrue(person.isRoot());
        assertFalse(person.isLeaf());
        assertEquals(0, person.depth());
        assertTrue(person.requiresInheritanceAnnotation());
        assertFalse(person.requiresExtends());

        ResolvedEntity student = model.lookupByName().get("Student");
        assertNotNull(student);
        assertFalse(student.isRoot());
        assertTrue(student.isLeaf());
        assertEquals(1, student.depth());
        assertFalse(student.requiresInheritanceAnnotation());
        assertTrue(student.requiresExtends());

        // Validate Topological Generation Order: Parent always before Child
        assertTrue(person.generationOrder() < student.generationOrder());
    }

    @Test
    void testMultipleChildren() {
        String source = """
            entity Person {}
            entity Student extends Person {}
            entity Teacher extends Person {}
        """;
        CompilationUnit cu = parseCDL(source);
        ResolvedInheritanceModel model = resolver.resolve(cu);

        assertEquals(3, model.orderedEntities().size());
        
        ResolvedEntity person = model.findByName("Person").orElseThrow();
        assertEquals(List.of("Student", "Teacher"), person.children());

        ResolvedEntity student = model.findByName("Student").orElseThrow();
        ResolvedEntity teacher = model.findByName("Teacher").orElseThrow();

        assertTrue(person.generationOrder() < student.generationOrder());
        assertTrue(person.generationOrder() < teacher.generationOrder());
    }

    @Test
    void testThreeLevelHierarchy() {
        String source = """
            entity Person {}
            entity Student extends Person {}
            entity GraduateStudent extends Student {}
        """;
        CompilationUnit cu = parseCDL(source);
        ResolvedInheritanceModel model = resolver.resolve(cu);

        assertEquals(3, model.orderedEntities().size());

        ResolvedEntity person = model.findByName("Person").orElseThrow();
        ResolvedEntity student = model.findByName("Student").orElseThrow();
        ResolvedEntity grad = model.findByName("GraduateStudent").orElseThrow();

        // Depths
        assertEquals(0, person.depth());
        assertEquals(1, student.depth());
        assertEquals(2, grad.depth());

        // Generation Orders
        assertTrue(person.generationOrder() < student.generationOrder());
        assertTrue(student.generationOrder() < grad.generationOrder());

        // Annotation Flags
        assertTrue(person.requiresInheritanceAnnotation()); // Root with kids
        assertTrue(student.requiresInheritanceAnnotation()); // Intermediate with kids
        assertFalse(grad.requiresInheritanceAnnotation()); // Leaf

        // Extends Flags
        assertFalse(person.requiresExtends()); // Root
        assertTrue(student.requiresExtends()); // Intermediate
        assertTrue(grad.requiresExtends()); // Leaf
    }

    @Test
    void testMultipleRoots() {
        String source = """
            entity Course {}
            entity Person {}
            entity Student extends Person {}
        """;
        CompilationUnit cu = parseCDL(source);
        ResolvedInheritanceModel model = resolver.resolve(cu);

        assertEquals(3, model.orderedEntities().size());
        assertEquals(2, model.rootEntities().size()); // Course and Person are roots

        assertTrue(model.lookupByName().get("Course").isRoot());
        assertTrue(model.lookupByName().get("Person").isRoot());
        assertFalse(model.lookupByName().get("Student").isRoot());
    }

    @Test
    void testLookupByEntityName() {
        String source = "entity Student {}";
        CompilationUnit cu = parseCDL(source);
        ResolvedInheritanceModel model = resolver.resolve(cu);

        assertTrue(model.findByName("Student").isPresent());
        assertFalse(model.findByName("NonExistent").isPresent());
    }

    @Test
    void testDefensiveCycleDetectionThrows() {
        // CDL parsed containing circular references (e.g. A extends B, B extends A)
        // Normally SemanticValidator prevents this, but InheritanceResolver validates this defensively
        String source = """
            entity A extends B {}
            entity B extends A {}
        """;
        
        // Skip SemanticValidator, pass raw AST straight to resolver to test defense
        CompilationUnit cu = parseCDL(source);
        assertThrows(InheritanceResolutionException.class, () -> resolver.resolve(cu));
    }

    @Test
    void testThreeNodeCycleDetectionThrows() {
        String source = """
            entity A extends B {}
            entity B extends C {}
            entity C extends A {}
        """;
        CompilationUnit cu = parseCDL(source);
        assertThrows(InheritanceResolutionException.class, () -> resolver.resolve(cu));
    }
}
