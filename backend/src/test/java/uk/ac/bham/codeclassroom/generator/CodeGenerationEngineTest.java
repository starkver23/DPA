package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.Test;
import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.engine.CodeGenerationEngine;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedFile;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedProject;
import uk.ac.bham.codeclassroom.generator.inheritance.InheritanceResolver;
import uk.ac.bham.codeclassroom.generator.inheritance.ResolvedInheritanceModel;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.token.Token;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenerationEngineTest {

    private final InheritanceResolver resolver = new InheritanceResolver();
    private final CodeGenerationEngine engine = new CodeGenerationEngine();

    private GeneratedProject generateProject(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        CompilationUnit cu = parser.parse();
        ResolvedInheritanceModel model = resolver.resolve(cu);
        return engine.generate(model);
    }

    @Test
    void testCodeGenerationPipeline() {
        String source = """
            entity Person {
                name String
            }
            entity Student extends Person {
                email String
                gpa BigDecimal
            }
        """;

        GeneratedProject project = generateProject(source);
        assertNotNull(project);
        
        // 7 generated files per entity, so 2 entities = 14 total files
        assertEquals(14, project.files().size());

        // Verify every component is generated for Person
        assertTrue(project.findByFilename("Person.java").isPresent());
        assertTrue(project.findByFilename("PersonRepository.java").isPresent());
        assertTrue(project.findByFilename("PersonService.java").isPresent());
        assertTrue(project.findByFilename("PersonServiceImpl.java").isPresent());
        assertTrue(project.findByFilename("PersonController.java").isPresent());
        assertTrue(project.findByFilename("PersonDTO.java").isPresent());
        assertTrue(project.findByFilename("PersonMapper.java").isPresent());

        // Verify every component is generated for Student
        assertTrue(project.findByFilename("Student.java").isPresent());
        assertTrue(project.findByFilename("StudentRepository.java").isPresent());
        assertTrue(project.findByFilename("StudentService.java").isPresent());
        assertTrue(project.findByFilename("StudentServiceImpl.java").isPresent());
        assertTrue(project.findByFilename("StudentController.java").isPresent());
        assertTrue(project.findByFilename("StudentDTO.java").isPresent());
        assertTrue(project.findByFilename("StudentMapper.java").isPresent());

        // Verify relative paths are generated correctly
        assertTrue(project.findByPath("src/main/java/uk/ac/bham/codeclassroom/model/Person.java").isPresent());
        assertTrue(project.findByPath("src/main/java/uk/ac/bham/codeclassroom/repository/PersonRepository.java").isPresent());

        // Verify root entity formatting, containing @Inheritance(strategy = InheritanceType.JOINED)
        GeneratedFile personEntity = project.findByFilename("Person.java").orElseThrow();
        assertTrue(personEntity.content().contains("public class Person"));
        assertTrue(personEntity.content().contains("@Inheritance(strategy = InheritanceType.JOINED)"));
        assertTrue(personEntity.content().contains("private String name;"));
        assertTrue(personEntity.content().contains("public String getName()"));

        // Verify subclass formatting, extending Person and having NO @Inheritance annotation
        GeneratedFile studentEntity = project.findByFilename("Student.java").orElseThrow();
        assertTrue(studentEntity.content().contains("public class Student extends Person"));
        assertFalse(studentEntity.content().contains("@Inheritance"));
        assertTrue(studentEntity.content().contains("private String email;"));
        assertTrue(studentEntity.content().contains("private BigDecimal gpa;"));
        assertTrue(studentEntity.content().contains("public BigDecimal getGpa()"));

        // Verify Controller paths mapping names to lowercase plurals (e.g. /api/students)
        GeneratedFile studentController = project.findByFilename("StudentController.java").orElseThrow();
        assertTrue(studentController.content().contains("@RequestMapping(\"/api/students\")"));

        // Verify DTO inheritance and properties
        GeneratedFile studentDto = project.findByFilename("StudentDTO.java").orElseThrow();
        assertTrue(studentDto.content().contains("public class StudentDTO extends PersonDTO"));
        assertTrue(studentDto.content().contains("private BigDecimal gpa;"));
        assertTrue(studentDto.content().contains("public BigDecimal getGpa()"));

        // Verify Repository formatting
        GeneratedFile studentRepo = project.findByFilename("StudentRepository.java").orElseThrow();
        assertTrue(studentRepo.content().contains("public interface StudentRepository extends JpaRepository<Student, Long>"));

        // Verify Mapper formatting and TODOs
        GeneratedFile studentMapper = project.findByFilename("StudentMapper.java").orElseThrow();
        assertTrue(studentMapper.content().contains("public class StudentMapper"));
        assertTrue(studentMapper.content().contains("// TODO: Implement field mappings"));
    }
}
