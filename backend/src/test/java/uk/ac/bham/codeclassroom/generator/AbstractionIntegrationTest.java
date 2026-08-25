package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLGenerator;
import uk.ac.bham.codeclassroom.generator.jdl.JDLEntity;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterAdapter;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProject;
import uk.ac.bham.codeclassroom.generator.jhipster.fullstack.FullStackGenerationPipeline;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractionIntegrationTest {

    private CompilationUnit parse(String source) {
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer.tokenize());
        CompilationUnit cu = parser.parse();
        new SemanticValidator().validate(cu);
        return cu;
    }

    private ExtendedJDLDocument processToJDL(String source) {
        CompilationUnit cu = parse(source);
        JHipsterAdapter adapter = new JHipsterAdapter();
        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "generatedApp", "com.mycompany.codeclassroom", "21", "3.2.5", "8.2.1", "postgresql", "jwt", "maven", "react"
        );
        JHipsterProject project = adapter.adapt(cu, config);
        ExtendedJDLGenerator jdlGen = new ExtendedJDLGenerator();
        return jdlGen.generate(project);
    }

    @Test
    public void test1_normalEntity() {
        String source = "entity Course { title String }";
        ExtendedJDLDocument doc = processToJDL(source);
        assertEquals(1, doc.entities().size());
        JDLEntity course = doc.entities().get(0);
        assertEquals("Course", course.name());
        assertFalse(course.abstractClass());
    }

    @Test
    public void test2_abstractEntity() {
        String source = "abstract entity Person { name String }";
        ExtendedJDLDocument doc = processToJDL(source);
        assertEquals(1, doc.entities().size());
        JDLEntity person = doc.entities().get(0);
        assertEquals("Person", person.name());
        assertTrue(person.abstractClass());
    }

    @Test
    public void test3_inheritance() {
        String source = "entity Person { name String } entity Student extends Person { studentNumber String }";
        ExtendedJDLDocument doc = processToJDL(source);
        assertEquals(2, doc.entities().size());
        Optional<JDLEntity> studentOpt = doc.entities().stream().filter(e -> e.name().equals("Student")).findFirst();
        assertTrue(studentOpt.isPresent());
        assertFalse(studentOpt.get().abstractClass());
        
        assertEquals(1, doc.inheritanceDeclarations().size());
        assertEquals("Student", doc.inheritanceDeclarations().get(0).childEntity());
        assertEquals("Person", doc.inheritanceDeclarations().get(0).parentEntity());
    }

    @Test
    public void test4_abstractParentConcreteChild() {
        String source = "abstract entity Person { name String } entity Student extends Person { studentNumber String }";
        ExtendedJDLDocument doc = processToJDL(source);
        assertEquals(2, doc.entities().size());
        
        JDLEntity person = doc.entities().stream().filter(e -> e.name().equals("Person")).findFirst().get();
        assertTrue(person.abstractClass());
        
        JDLEntity student = doc.entities().stream().filter(e -> e.name().equals("Student")).findFirst().get();
        assertFalse(student.abstractClass());
        
        assertEquals(1, doc.inheritanceDeclarations().size());
        assertEquals("Student", doc.inheritanceDeclarations().get(0).childEntity());
        assertEquals("Person", doc.inheritanceDeclarations().get(0).parentEntity());
    }

    @Test
    public void test5_exactCompleteExample() {
        String source = "abstract entity Person { name String email String } " +
                        "entity Student extends Person { studentNumber String } " +
                        "entity Course { title String code String } " +
                        "relationship ManyToMany { Student{courses} to Course{students} }";
        ExtendedJDLDocument doc = processToJDL(source);
        assertEquals(3, doc.entities().size());
        
        JDLEntity person = doc.entities().stream().filter(e -> e.name().equals("Person")).findFirst().get();
        assertTrue(person.abstractClass());
        
        JDLEntity student = doc.entities().stream().filter(e -> e.name().equals("Student")).findFirst().get();
        assertFalse(student.abstractClass());
        
        JDLEntity course = doc.entities().stream().filter(e -> e.name().equals("Course")).findFirst().get();
        assertFalse(course.abstractClass());
        
        assertEquals(1, doc.inheritanceDeclarations().size());
        assertEquals("Person", doc.inheritanceDeclarations().get(0).parentEntity());
        assertEquals("Student", doc.inheritanceDeclarations().get(0).childEntity());
        
        assertEquals(1, doc.relationships().size());
        assertEquals("Student", doc.relationships().get(0).sourceEntity());
        assertEquals("Course", doc.relationships().get(0).targetEntity());
    }

    @Test
    public void test6_generatedJava(@TempDir Path tempDir) throws Exception {
        String source = "abstract entity Person { name String email String } " +
                        "entity Student extends Person { studentNumber String } " +
                        "entity Course { title String code String } " +
                        "relationship ManyToMany { Student{courses} to Course{students} }";
        
        ExtendedJDLDocument doc = processToJDL(source);
        
        FullStackGenerationPipeline pipeline = new FullStackGenerationPipeline();
        Path currentDir = Path.of("").toAbsolutePath();
        Path parentDir = currentDir.getParent();
        Path generatorJhipsterPath = parentDir.resolve("generator-jhipster");
        if (Files.exists(generatorJhipsterPath)) {
            pipeline.setJHipsterForkPath(parentDir.toString());
        }
        
        Path outputDir = tempDir.resolve("gen-app");
        Files.createDirectories(outputDir);
        
        pipeline.generate(doc, outputDir);
        
        Path domainDir = outputDir.resolve("src/main/java/com/mycompany/codeclassroom/domain");
        assertTrue(Files.exists(domainDir), "Domain directory should exist");
        
        Path personFile = domainDir.resolve("Person.java");
        assertTrue(Files.exists(personFile), "Person.java should exist");
        String personContent = Files.readString(personFile);
        assertTrue(personContent.contains("public abstract class Person"), "Person should be abstract");
        
        Path studentFile = domainDir.resolve("Student.java");
        assertTrue(Files.exists(studentFile), "Student.java should exist");
        String studentContent = Files.readString(studentFile);
        assertTrue(studentContent.contains("public class Student extends Person"), "Student should extend Person");
        
        Path courseFile = domainDir.resolve("Course.java");
        assertTrue(Files.exists(courseFile), "Course.java should exist");
        String courseContent = Files.readString(courseFile);
        assertTrue(courseContent.contains("public class Course"), "Course should be a normal class");
    }

    @Test
    public void test7_interfacesGeneratedJava(@TempDir Path tempDir) throws Exception {
        String source = "abstract entity Person { name String email String } " +
                        "interface Payable { calculateSalary() Double } " +
                        "entity Student extends Person implements Payable { studentNumber String calculateSalary() Double } " +
                        "entity Course { title String code String } " +
                        "relationship ManyToMany { Student{courses} to Course{students} }";
        
        ExtendedJDLDocument doc = processToJDL(source);
        
        FullStackGenerationPipeline pipeline = new FullStackGenerationPipeline();
        Path currentDir = Path.of("").toAbsolutePath();
        Path parentDir = currentDir.getParent();
        Path generatorJhipsterPath = parentDir.resolve("generator-jhipster");
        if (Files.exists(generatorJhipsterPath)) {
            pipeline.setJHipsterForkPath(parentDir.toString());
        }
        
        Path outputDir = tempDir.resolve("gen-app");
        Files.createDirectories(outputDir);
        
        pipeline.generate(doc, outputDir);
        
        Path domainDir = outputDir.resolve("src/main/java/com/mycompany/codeclassroom/domain");
        assertTrue(Files.exists(domainDir), "Domain directory should exist");
        
        Path payableFile = domainDir.resolve("Payable.java");
        assertTrue(Files.exists(payableFile), "Payable.java should exist");
        String payableContent = Files.readString(payableFile);
        assertTrue(payableContent.contains("public interface Payable"), "Payable should be a Java interface");
        assertTrue(payableContent.contains("Double calculateSalary()"), "Payable should declare calculateSalary() method");
        
        Path studentFile = domainDir.resolve("Student.java");
        assertTrue(Files.exists(studentFile), "Student.java should exist");
        String studentContent = Files.readString(studentFile);
        assertTrue(studentContent.contains("public class Student extends Person implements Payable"), "Student should implement Payable");
        assertTrue(studentContent.contains("public Double calculateSalary()"), "Student should provide calculateSalary() implementation");
    }
}
