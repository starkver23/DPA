package uk.ac.bham.codeclassroom.generator.jdl;

import org.junit.jupiter.api.Test;
import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterAdapter;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProject;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.token.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedJDLTest {

    private final JHipsterAdapter adapter = new JHipsterAdapter();
    private final ExtendedJDLGenerator generator = new ExtendedJDLGenerator();
    private final JDLSerializer serializer = new JDLSerializer();
    private final ExtendedJDLProcessor processor = new ExtendedJDLProcessor();

    private CompilationUnit parseCDL(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    @Test
    void testEntitiesAndFieldsPreservation() {
        String source = """
            entity Student {
                age Integer
                name String
            }
        """;
        CompilationUnit cu = parseCDL(source);
        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "MyTestApp", "com.test", "21", "3.2.5", "8.2.1", "postgresql", "jwt", "maven", "react"
        );
        JHipsterProject jhipsterProject = adapter.adapt(cu, config);
        ExtendedJDLDocument jdlDoc = generator.generate(jhipsterProject);

        assertNotNull(jdlDoc);
        assertEquals(1, jdlDoc.entities().size());

        JDLEntity student = jdlDoc.entities().get(0);
        assertEquals("Student", student.name());
        assertEquals(2, student.fields().size());

        // Field mapping checks
        assertEquals("age", student.fields().get(0).name());
        assertEquals("Integer", student.fields().get(0).type());
        assertEquals("name", student.fields().get(1).name());
        assertEquals("String", student.fields().get(1).type());

        String serialized = serializer.serialize(jdlDoc);
        assertTrue(serialized.contains("entity Student {"));
        assertTrue(serialized.contains("    age Integer"));
        assertTrue(serialized.contains("    name String"));
        assertTrue(serialized.contains("baseName MyTestApp"));
    }

    @Test
    void testRelationshipsPreservation() {
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
        JHipsterProject jhipsterProject = adapter.adapt(cu, config);
        ExtendedJDLDocument jdlDoc = generator.generate(jhipsterProject);

        assertNotNull(jdlDoc);
        assertFalse(jdlDoc.relationships().isEmpty());

        String serialized = serializer.serialize(jdlDoc);

        // Verify relationship serialization
        assertTrue(serialized.contains("relationship OneToOne {"));
        assertTrue(serialized.contains("relationship OneToMany {"));
        assertTrue(serialized.contains("relationship ManyToOne {"));
        assertTrue(serialized.contains("relationship ManyToMany {"));

        // Verify navigation property preservation
        assertTrue(serialized.contains("Student to Address{student}"));
        assertTrue(serialized.contains("Department{professors} to Professor"));
        assertTrue(serialized.contains("Student{courses} to Course{students}"));
    }

    @Test
    void testInheritancePreservation() {
        String source = """
            entity Person {
                id Long
            }
            entity Student extends Person {
                grade String
            }
            entity Teacher extends Person {
                salary Double
            }
            entity GraduateStudent extends Student {
                thesis String
            }
        """;
        CompilationUnit cu = parseCDL(source);
        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("InheritApp");
        JHipsterProject jhipsterProject = adapter.adapt(cu, config);
        ExtendedJDLDocument jdlDoc = generator.generate(jhipsterProject);

        assertNotNull(jdlDoc);
        assertEquals(3, jdlDoc.inheritanceDeclarations().size());

        String serialized = serializer.serialize(jdlDoc);

        // Verify that inheritance information is preserved separately
        assertTrue(serialized.contains("// CodeClassroom Extension"));
        assertTrue(serialized.contains("inheritance {"));
        assertTrue(serialized.contains("    GraduateStudent extends Student"));
        assertTrue(serialized.contains("    Student extends Person"));
        assertTrue(serialized.contains("    Teacher extends Person"));

        // Verify normal entity declarations are not modified (extends is NOT embedded inside the normal standard JDL block)
        assertTrue(serialized.contains("entity Student {"));
        assertFalse(serialized.contains("entity Student extends Person {"));
    }

    @Test
    void testDeterministicFormatting() {
        String source1 = """
            entity B {
                y String
                x Integer
            }
            entity A {
                b String
                a String
            }
        """;

        String source2 = """
            entity A {
                a String
                b String
            }
            entity B {
                x Integer
                y String
            }
        """;

        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("DeterministicApp");

        String serialized1 = serializer.serialize(generator.generate(adapter.adapt(parseCDL(source1), config)));
        String serialized2 = serializer.serialize(generator.generate(adapter.adapt(parseCDL(source2), config)));

        assertEquals(serialized1, serialized2);
    }

    @Test
    void testConfigurationPreserved() {
        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "CustomJDLApp", "org.bham", "17", "3.1.2", "8.0.0", "mysql", "oauth2", "gradle", "angular"
        );
        CompilationUnit cu = parseCDL("entity Dummy {}");
        JHipsterProject jhipsterProject = adapter.adapt(cu, config);
        ExtendedJDLDocument jdlDoc = generator.generate(jhipsterProject);

        String serialized = serializer.serialize(jdlDoc);

        assertTrue(serialized.contains("baseName CustomJDLApp"));
        assertTrue(serialized.contains("packageName org.bham"));
        assertTrue(serialized.contains("prodDatabaseType mysql"));
        assertTrue(serialized.contains("authenticationType oauth2"));
        assertTrue(serialized.contains("buildTool gradle"));
        assertTrue(serialized.contains("clientFramework angular"));
    }

    @Test
    void testEntitiesClauseInApplicationBlock() {
        String source = """
            entity Professor {
                name String
            }
            entity GraduateProject {
                title String
            }
            entity Student {
                gpa Double
            }
        """;
        CompilationUnit cu = parseCDL(source);
        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("TestApp");
        JHipsterProject jhipsterProject = adapter.adapt(cu, config);
        ExtendedJDLDocument jdlDoc = generator.generate(jhipsterProject);

        assertNotNull(jdlDoc);
        assertTrue(jdlDoc.hasEntities());
        assertEquals("GraduateProject, Professor, Student", jdlDoc.formattedEntitiesList());

        String serialized = serializer.serialize(jdlDoc);

        // Verify entities line exists inside the application block
        assertTrue(serialized.contains("entities GraduateProject, Professor, Student"));

        // Extract the exact entities line from serialized text
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("entities\\s+([^\\n]+)");
        java.util.regex.Matcher matcher = pattern.matcher(serialized);
        assertTrue(matcher.find());
        
        String entitiesClause = matcher.group(1).trim();
        String[] entities = entitiesClause.split(",\\s*");
        
        assertEquals(3, entities.length);
        assertEquals("GraduateProject", entities[0]);
        assertEquals("Professor", entities[1]);
        assertEquals("Student", entities[2]);
    }

    @Test
    void testEmptyProject() {
        CompilationUnit cu = parseCDL("");
        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("EmptyApp");
        JHipsterProject jhipsterProject = adapter.adapt(cu, config);
        ExtendedJDLDocument jdlDoc = generator.generate(jhipsterProject);

        assertNotNull(jdlDoc);
        assertTrue(jdlDoc.entities().isEmpty());
        assertTrue(jdlDoc.relationships().isEmpty());
        assertTrue(jdlDoc.inheritanceDeclarations().isEmpty());

        String serialized = serializer.serialize(jdlDoc);
        assertTrue(serialized.contains("baseName EmptyApp"));
        assertFalse(serialized.contains("entity"));
        assertFalse(serialized.contains("relationship"));
        assertFalse(serialized.contains("inheritance"));
    }

    @Test
    void testInvalidInputsThrow() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null));
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(null));
        assertThrows(IllegalArgumentException.class, () -> processor.process(null));
    }

    @Test
    void testStringBuilderNotUsed() throws IOException {
        // Assert that the source file for JDLSerializer does not contain "StringBuilder"
        Path serializerSource = Paths.get("src/main/java/uk/ac/bham/codeclassroom/generator/jdl/JDLSerializer.java");
        if (!Files.exists(serializerSource)) {
            // Under test context, sometimes path is relative to backend root
            serializerSource = Paths.get("backend/src/main/java/uk/ac/bham/codeclassroom/generator/jdl/JDLSerializer.java");
        }
        assertTrue(Files.exists(serializerSource), "JDLSerializer.java must exist");
        String content = Files.readString(serializerSource);
        assertFalse(content.contains("StringBuilder"), "JDLSerializer must not use StringBuilder");
    }

    @Test
    void testExtendedJDLProcessorSeparation() {
        String source = """
            entity Person {
                id Long
            }
            entity Student extends Person {
                grade String
            }
        """;
        CompilationUnit cu = parseCDL(source);
        JHipsterProjectConfiguration config = JHipsterProjectConfiguration.createDefault("ProcessorApp");
        JHipsterProject jhipsterProject = adapter.adapt(cu, config);
        ExtendedJDLDocument jdlDoc = generator.generate(jhipsterProject);

        SeparatedJDLResult result = processor.process(jdlDoc);

        assertNotNull(result);
        assertNotNull(result.standardJDLDocument());
        assertNotNull(result.inheritanceDeclarations());

        // Standard document should contain the standard structures
        assertEquals(2, result.standardJDLDocument().entities().size());
        assertEquals("Person", result.standardJDLDocument().entities().get(0).name());
        assertEquals("Student", result.standardJDLDocument().entities().get(1).name());

        // Standard JDL document must NOT have any inheritance block (it's set to empty)
        assertTrue(result.standardJDLDocument().inheritanceDeclarations().isEmpty());
        assertFalse(result.standardJDLDocument().hasInheritances());

        // Inheritance metadata should be separately preserved
        assertEquals(1, result.inheritanceDeclarations().size());
        JDLInheritance inheritance = result.inheritanceDeclarations().get(0);
        assertEquals("Student", inheritance.childEntity());
        assertEquals("Person", inheritance.parentEntity());

        // Serializer output of the standard JDL document must NOT contain the inheritance block
        String standardSerialized = serializer.serialize(result.standardJDLDocument());
        assertFalse(standardSerialized.contains("inheritance {"));
        assertFalse(standardSerialized.contains("// CodeClassroom Extension"));
    }
}
