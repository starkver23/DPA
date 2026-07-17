package uk.ac.bham.codeclassroom.generator.jhipster.template;

import org.junit.jupiter.api.Test;
import uk.ac.bham.codeclassroom.generator.ast.CompilationUnit;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedFile;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedProject;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterAdapter;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProject;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;
import uk.ac.bham.codeclassroom.generator.lexer.Lexer;
import uk.ac.bham.codeclassroom.generator.parser.Parser;
import uk.ac.bham.codeclassroom.generator.token.Token;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JHipsterTemplateEngineTest {

    private final JHipsterAdapter adapter = new JHipsterAdapter();
    private final JHipsterTemplateEngine templateEngine = new JHipsterTemplateEngine();

    private CompilationUnit parseCDL(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    @Test
    void testJHipsterGeneratorPipeline() {
        String source = """
            entity Person {
                name String
            }
            entity Student extends Person {
                gpa Double
            }
            entity Undergrad extends Student {
                major String
            }
            entity Teacher {
                salary Double
            }
        """;

        CompilationUnit cu = parseCDL(source);
        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "TestApp", "com.mycompany.myapp", "21", "3.2.5", "8.2.1", "postgresql", "jwt", "maven", "react"
        );
        JHipsterProject project = adapter.adapt(cu, config);
        GeneratedProject generated = templateEngine.generate(project);

        assertNotNull(generated);
        // 4 entities, each receives 7 generated files (Entity, Repository, Service, ServiceImpl, Controller/Resource, DTO, Mapper)
        // 4 * 7 = 28 generated files
        assertEquals(28, generated.files().size());

        // --- 1. Root Entity (Person) ---
        GeneratedFile personEntity = generated.findByFilename("Person.java").orElseThrow();
        assertTrue(personEntity.content().contains("package com.mycompany.myapp.domain;"));
        assertTrue(personEntity.content().contains("public class Person"));
        assertTrue(personEntity.content().contains("@Inheritance(strategy = InheritanceType.JOINED)"));
        assertTrue(personEntity.content().contains("private String name;"));
        assertFalse(personEntity.content().contains("extends"));

        // --- 2. Child Entity (Student) ---
        GeneratedFile studentEntity = generated.findByFilename("Student.java").orElseThrow();
        assertTrue(studentEntity.content().contains("public class Student extends Person"));
        assertFalse(studentEntity.content().contains("@Inheritance"));
        assertTrue(studentEntity.content().contains("private Double gpa;"));

        // --- 3. Grandchild Entity (Undergrad) ---
        GeneratedFile undergradEntity = generated.findByFilename("Undergrad.java").orElseThrow();
        assertTrue(undergradEntity.content().contains("public class Undergrad extends Student"));
        assertFalse(undergradEntity.content().contains("@Inheritance"));
        assertTrue(undergradEntity.content().contains("private String major;"));

        // --- 4. Unrelated Root (Teacher) ---
        GeneratedFile teacherEntity = generated.findByFilename("Teacher.java").orElseThrow();
        assertTrue(teacherEntity.content().contains("public class Teacher"));
        assertFalse(teacherEntity.content().contains("@Inheritance")); // has no children, so no inheritance annotations are needed
        assertFalse(teacherEntity.content().contains("extends"));

        // --- 5. Repositories Generated Correctly ---
        for (String name : List.of("Person", "Student", "Undergrad", "Teacher")) {
            GeneratedFile repo = generated.findByFilename(name + "Repository.java").orElseThrow();
            assertTrue(repo.content().contains("package com.mycompany.myapp.repository;"));
            assertTrue(repo.content().contains("public interface " + name + "Repository extends JpaRepository<" + name + ", Long>"));
        }

        // --- 6. Services & ServiceImpls Generated Correctly ---
        for (String name : List.of("Person", "Student", "Undergrad", "Teacher")) {
            GeneratedFile service = generated.findByFilename(name + "Service.java").orElseThrow();
            assertTrue(service.content().contains("package com.mycompany.myapp.service;"));
            assertTrue(service.content().contains("public interface " + name + "Service"));

            GeneratedFile serviceImpl = generated.findByFilename(name + "ServiceImpl.java").orElseThrow();
            assertTrue(serviceImpl.content().contains("package com.mycompany.myapp.service.impl;"));
            assertTrue(serviceImpl.content().contains("public class " + name + "ServiceImpl implements " + name + "Service"));
        }

        // --- 7. DTO Inheritance Preserved ---
        GeneratedFile personDto = generated.findByFilename("PersonDTO.java").orElseThrow();
        assertFalse(personDto.content().contains("extends"));

        GeneratedFile studentDto = generated.findByFilename("StudentDTO.java").orElseThrow();
        assertTrue(studentDto.content().contains("public class StudentDTO extends PersonDTO"));

        GeneratedFile undergradDto = generated.findByFilename("UndergradDTO.java").orElseThrow();
        assertTrue(undergradDto.content().contains("public class UndergradDTO extends StudentDTO"));

        // --- 8. Mapper Inheritance Preserved and mapped (Inheritance-Aware Mapping) ---
        // Student has local field: gpa
        // Student has inherited field from Person: name
        GeneratedFile studentMapper = generated.findByFilename("StudentMapper.java").orElseThrow();
        assertTrue(studentMapper.content().contains("package com.mycompany.myapp.service.mapper;"));
        assertTrue(studentMapper.content().contains("dto.setGpa(entity.getGpa());")); // local field
        assertTrue(studentMapper.content().contains("dto.setName(entity.getName());")); // inherited field
        assertTrue(studentMapper.content().contains("entity.setGpa(dto.getGpa());")); // local field
        assertTrue(studentMapper.content().contains("entity.setName(dto.getName());")); // inherited field

        // Undergrad has local field: major
        // Undergrad has inherited fields from Student/Person: gpa, name
        GeneratedFile undergradMapper = generated.findByFilename("UndergradMapper.java").orElseThrow();
        assertTrue(undergradMapper.content().contains("dto.setMajor(entity.getMajor());")); // local
        assertTrue(undergradMapper.content().contains("dto.setGpa(entity.getGpa());")); // inherited
        assertTrue(undergradMapper.content().contains("dto.setName(entity.getName());")); // inherited

        // --- 9. Controller Generation (Resource) ---
        for (String name : List.of("Person", "Student", "Undergrad", "Teacher")) {
            GeneratedFile controller = generated.findByFilename(name + "Resource.java").orElseThrow();
            assertTrue(controller.content().contains("package com.mycompany.myapp.web.rest;"));
            assertTrue(controller.content().contains("public class " + name + "Resource"));
        }
    }

    @Test
    void testInvalidInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> templateEngine.generate(null));

        TemplateModelBuilder builder = new TemplateModelBuilder();
        assertThrows(IllegalArgumentException.class, () -> builder.build(null, null));
    }
}
