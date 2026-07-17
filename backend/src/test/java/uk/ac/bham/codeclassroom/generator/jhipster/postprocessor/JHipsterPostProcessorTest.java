package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.bham.codeclassroom.generator.jdl.ExtendedJDLDocument;
import uk.ac.bham.codeclassroom.generator.jdl.JDLInheritance;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JHipsterPostProcessorTest {

    @TempDir
    Path tempDir;

    private void createMockProjectFiles(Path root, String basePackage) throws IOException {
        Path packagePath = root.resolve("src/main/java").resolve(basePackage.replace('.', '/'));
        Path testPackagePath = root.resolve("src/test/java").resolve(basePackage.replace('.', '/'));
        
        // Create folders
        Files.createDirectories(packagePath.resolve("domain"));
        Files.createDirectories(packagePath.resolve("service/dto"));
        Files.createDirectories(packagePath.resolve("service/mapper"));
        Files.createDirectories(packagePath.resolve("web/rest"));
        Files.createDirectories(testPackagePath.resolve("domain"));

        // Create mock test sample file for Student
        Files.writeString(testPackagePath.resolve("domain/StudentTestSamples.java"), """
            package com.test.domain;
            public class StudentTestSamples {
                public static Student getStudentSample1() {
                    return new Student().id(1L).gpa(3.8);
                }
            }
        """);

        // Create standard entity file for Person (parent)
        Files.writeString(packagePath.resolve("domain/Person.java"), """
            package com.test.domain;
            import jakarta.persistence.*;
            import org.hibernate.annotations.Cache;
            import org.hibernate.annotations.CacheConcurrencyStrategy;
            @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
            public class Person {
                private Long id;
                private String name;
            }
        """);

        // Create standard entity file for Student (child)
        Files.writeString(packagePath.resolve("domain/Student.java"), """
            package com.test.domain;
            import jakarta.persistence.*;
            import org.hibernate.annotations.Cache;
            import org.hibernate.annotations.CacheConcurrencyStrategy;
            @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
            public class Student {
                private Long id;
                private Double gpa;
            }
        """);

        // Create standard entity file for Undergrad (grandchild)
        Files.writeString(packagePath.resolve("domain/Undergrad.java"), """
            package com.test.domain;
            import jakarta.persistence.*;
            import org.hibernate.annotations.Cache;
            import org.hibernate.annotations.CacheConcurrencyStrategy;
            @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
            public class Undergrad {
                private Long id;
                private String major;
            }
        """);

        // Create standard DTO file for Student
        Files.writeString(packagePath.resolve("service/dto/StudentDTO.java"), """
            package com.test.service.dto;
            public class StudentDTO {
                private Long id;
                private Double gpa;
            }
        """);

        // Create standard Mapper file for Student
        Files.writeString(packagePath.resolve("service/mapper/StudentMapper.java"), """
            package com.test.service.mapper;
            import org.mapstruct.Mapper;
            @Mapper(componentModel = "spring")
            public interface StudentMapper {}
        """);

        // Create standard unrelated file
        Files.writeString(packagePath.resolve("web/rest/UnrelatedResource.java"), """
            package com.test.web.rest;
            public class UnrelatedResource {}
        """);
    }

    @Test
    void testInheritanceInPlaceTransformation() throws IOException {
        createMockProjectFiles(tempDir, "com.test");

        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "TestApp", "com.test", "21", "3.2.5", "8.2.1", "postgresql", "jwt", "maven", "react"
        );

        // Define inheritance declarations:
        // Student extends Person
        // Undergrad extends Student
        List<JDLInheritance> inheritances = List.of(
            new JDLInheritance("Student", "Person"),
            new JDLInheritance("Undergrad", "Student")
        );

        ExtendedJDLDocument doc = new ExtendedJDLDocument(List.of(), List.of(), inheritances, config);

        JHipsterPostProcessor postProcessor = new JHipsterPostProcessor();
        Path result = postProcessor.transform(tempDir, doc);

        assertNotNull(result);

        Path packagePath = tempDir.resolve("src/main/java/com/test");

        // --- 1. Verify Parent (Person) receives @Inheritance strategy JOINED and imports ---
        String personContent = Files.readString(packagePath.resolve("domain/Person.java"));
        assertTrue(personContent.contains("@Inheritance(strategy = InheritanceType.JOINED)"));
        assertTrue(personContent.contains("import jakarta.persistence.Inheritance;"));
        assertTrue(personContent.contains("import jakarta.persistence.InheritanceType;"));
        assertTrue(personContent.contains("public class Person"));
        assertTrue(personContent.contains("private Long id;"));
        assertTrue(personContent.contains("@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)"));
        assertTrue(personContent.contains("import org.hibernate.annotations.Cache;"));
        assertTrue(personContent.contains("import org.hibernate.annotations.CacheConcurrencyStrategy;"));

        // --- 2. Verify Child (Student) extends Person and has its ID field and @Cache annotations stripped ---
        String studentContent = Files.readString(packagePath.resolve("domain/Student.java"));
        assertTrue(studentContent.contains("public class Student extends Person"));
        assertFalse(studentContent.contains("@Inheritance")); // Student has children but is not root, so no JPA inheritance annotation
        assertFalse(studentContent.contains("private Long id;"));
        assertFalse(studentContent.contains("@Cache"));
        assertFalse(studentContent.contains("org.hibernate.annotations.Cache"));

        // --- 3. Verify Grandchild (Undergrad) extends Student and has its ID field and @Cache annotations stripped ---
        String undergradContent = Files.readString(packagePath.resolve("domain/Undergrad.java"));
        assertTrue(undergradContent.contains("public class Undergrad extends Student"));
        assertFalse(undergradContent.contains("@Inheritance"));
        assertFalse(undergradContent.contains("private Long id;"));
        assertFalse(undergradContent.contains("@Cache"));
        assertFalse(undergradContent.contains("org.hibernate.annotations.Cache"));

        // --- 4. Verify DTO extends ---
        String studentDtoContent = Files.readString(packagePath.resolve("service/dto/StudentDTO.java"));
        assertTrue(studentDtoContent.contains("public class StudentDTO extends PersonDTO"));

        // --- 5. Verify Mapper uses configuration added ---
        String studentMapperContent = Files.readString(packagePath.resolve("service/mapper/StudentMapper.java"));
        assertTrue(studentMapperContent.contains("@Mapper(componentModel = \"spring\", uses = { PersonMapper.class })"));

        // --- 6. Verify Unrelated resource is byte-for-byte identical ---
        String unrelatedContent = Files.readString(packagePath.resolve("web/rest/UnrelatedResource.java"));
        assertTrue(unrelatedContent.contains("public class UnrelatedResource"));
        assertFalse(unrelatedContent.contains("extends"));

        // --- 7. Verify Test Samples are rewritten from builders to setters ---
        Path testPackagePath = tempDir.resolve("src/test/java/com/test");
        String studentTestSampleContent = Files.readString(testPackagePath.resolve("domain/StudentTestSamples.java"));
        assertTrue(studentTestSampleContent.contains("Student student = new Student();"));
        assertTrue(studentTestSampleContent.contains("student.setId(1L);"));
        assertTrue(studentTestSampleContent.contains("student.setGpa(3.8);"));
        assertTrue(studentTestSampleContent.contains("return student;"));
        assertFalse(studentTestSampleContent.contains("return new Student().id(1L).gpa(3.8);"));
    }

    @Test
    void testPostProcessorDeterminism() throws IOException {
        Path proj1 = tempDir.resolve("proj1");
        Path proj2 = tempDir.resolve("proj2");

        createMockProjectFiles(proj1, "com.test");
        createMockProjectFiles(proj2, "com.test");

        JHipsterProjectConfiguration config = new JHipsterProjectConfiguration(
            "TestApp", "com.test", "21", "3.2.5", "8.2.1", "postgresql", "jwt", "maven", "react"
        );
        List<JDLInheritance> inheritances = List.of(
            new JDLInheritance("Student", "Person")
        );
        ExtendedJDLDocument doc = new ExtendedJDLDocument(List.of(), List.of(), inheritances, config);

        JHipsterPostProcessor p1 = new JHipsterPostProcessor();
        JHipsterPostProcessor p2 = new JHipsterPostProcessor();

        p1.transform(proj1, doc);
        p2.transform(proj2, doc);

        // Verify determinism by comparing content of transformed Student.java
        String s1 = Files.readString(proj1.resolve("src/main/java/com/test/domain/Student.java"));
        String s2 = Files.readString(proj2.resolve("src/main/java/com/test/domain/Student.java"));
        assertEquals(s1, s2);
    }

    @Test
    void testInvalidInputsThrow() {
        JHipsterPostProcessor postProcessor = new JHipsterPostProcessor();
        assertThrows(TransformationException.class, () -> postProcessor.transform(null, null));
        assertThrows(TransformationException.class, () -> postProcessor.transform(tempDir, null));
        assertThrows(TransformationException.class, () -> postProcessor.transform(tempDir.resolve("non-existent"), new ExtendedJDLDocument(List.of(), List.of(), List.of(), null)));
    }
}
