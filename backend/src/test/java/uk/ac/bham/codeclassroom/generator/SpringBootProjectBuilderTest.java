package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedFile;
import uk.ac.bham.codeclassroom.generator.engine.GeneratedProject;
import uk.ac.bham.codeclassroom.generator.project.ProjectBuilderException;
import uk.ac.bham.codeclassroom.generator.project.ProjectMetadata;
import uk.ac.bham.codeclassroom.generator.project.SpringBootProjectBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpringBootProjectBuilderTest {

    private final SpringBootProjectBuilder builder = new SpringBootProjectBuilder();

    @Test
    void testProjectBuilderBuildPipeline(@TempDir Path tempDir) throws IOException {
        // Create simple mock GeneratedProject
        GeneratedFile entity = new GeneratedFile(
                "Student.java",
                "src/main/java/uk/ac/bham/codeclassroom/model/Student.java",
                "package uk.ac.bham.codeclassroom.model;\npublic class Student {}"
        );
        GeneratedFile repository = new GeneratedFile(
                "StudentRepository.java",
                "src/main/java/uk/ac/bham/codeclassroom/repository/StudentRepository.java",
                "package uk.ac.bham.codeclassroom.repository;\nimport uk.ac.bham.codeclassroom.model.Student;\npublic interface StudentRepository {}"
        );
        GeneratedFile service = new GeneratedFile(
                "StudentService.java",
                "src/main/java/uk/ac/bham/codeclassroom/service/StudentService.java",
                "package uk.ac.bham.codeclassroom.service;\npublic interface StudentService {}"
        );
        GeneratedFile serviceImpl = new GeneratedFile(
                "StudentServiceImpl.java",
                "src/main/java/uk/ac/bham/codeclassroom/service/impl/StudentServiceImpl.java",
                "package uk.ac.bham.codeclassroom.service.impl;\nimport uk.ac.bham.codeclassroom.service.StudentService;\npublic class StudentServiceImpl implements StudentService {}"
        );
        GeneratedFile controller = new GeneratedFile(
                "StudentController.java",
                "src/main/java/uk/ac/bham/codeclassroom/controller/StudentController.java",
                "package uk.ac.bham.codeclassroom.controller;\npublic class StudentController {}"
        );
        GeneratedFile dto = new GeneratedFile(
                "StudentDTO.java",
                "src/main/java/uk/ac/bham/codeclassroom/dto/StudentDTO.java",
                "package uk.ac.bham.codeclassroom.dto;\npublic class StudentDTO {}"
        );
        GeneratedFile mapper = new GeneratedFile(
                "StudentMapper.java",
                "src/main/java/uk/ac/bham/codeclassroom/mapper/StudentMapper.java",
                "package uk.ac.bham.codeclassroom.mapper;\npublic class StudentMapper {}"
        );

        GeneratedProject generatedProject = new GeneratedProject(List.of(
                entity, repository, service, serviceImpl, controller, dto, mapper
        ));

        // Create metadata with custom package
        ProjectMetadata metadata = new ProjectMetadata(
                "StudentSystem",
                "com.example",
                "student-system",
                "com.example.studentsystem",
                "21",
                "3.5.0"
        );

        // Run build
        Path projectPath = builder.build(generatedProject, metadata, tempDir);

        // 1. Verify project directory created
        assertNotNull(projectPath);
        assertTrue(Files.exists(projectPath));
        assertEquals("StudentSystem", projectPath.getFileName().toString());

        // 2. Verify folder hierarchy created
        Path pomFile = projectPath.resolve("pom.xml");
        Path propertiesFile = projectPath.resolve("src/main/resources/application.properties");
        Path staticDir = projectPath.resolve("src/main/resources/static");
        Path templatesDir = projectPath.resolve("src/main/resources/templates");
        Path packageDir = projectPath.resolve("src/main/java/com/example/studentsystem");

        assertTrue(Files.exists(pomFile));
        assertTrue(Files.exists(propertiesFile));
        assertTrue(Files.exists(staticDir));
        assertTrue(Files.exists(templatesDir));
        assertTrue(Files.exists(packageDir));

        // 3. Verify pom.xml generated correctly
        String pomContent = Files.readString(pomFile);
        assertTrue(pomContent.contains("<groupId>com.example</groupId>"));
        assertTrue(pomContent.contains("<artifactId>student-system</artifactId>"));
        assertTrue(pomContent.contains("<name>StudentSystem</name>"));
        assertTrue(pomContent.contains("<java.version>21</java.version>"));
        assertTrue(pomContent.contains("<version>3.5.0</version>"));
        assertTrue(pomContent.contains("spring-boot-starter-data-jpa"));
        assertTrue(pomContent.contains("spring-boot-starter-web"));
        assertTrue(pomContent.contains("spring-boot-starter-validation"));
        assertTrue(pomContent.contains("h2"));
        assertTrue(pomContent.contains("lombok"));

        // 4. Verify application.properties generated correctly
        String propContent = Files.readString(propertiesFile);
        assertTrue(propContent.contains("spring.application.name=StudentSystem"));
        assertTrue(propContent.contains("spring.datasource.url=jdbc:h2:mem:testdb"));
        assertTrue(propContent.contains("spring.h2.console.enabled=true"));

        // 5. Verify GeneratedApplication.java generated correctly
        Path mainAppFile = packageDir.resolve("GeneratedApplication.java");
        assertTrue(Files.exists(mainAppFile));
        String mainAppContent = Files.readString(mainAppFile);
        assertTrue(mainAppContent.contains("package com.example.studentsystem;"));
        assertTrue(mainAppContent.contains("@SpringBootApplication"));
        assertTrue(mainAppContent.contains("public class GeneratedApplication"));

        // 6. Verify entity files written with package updated
        Path modelDir = packageDir.resolve("model");
        assertTrue(Files.exists(modelDir));
        Path studentFile = modelDir.resolve("Student.java");
        assertTrue(Files.exists(studentFile));
        String studentContent = Files.readString(studentFile);
        assertTrue(studentContent.contains("package com.example.studentsystem.model;"));
        assertFalse(studentContent.contains("uk.ac.bham.codeclassroom"));

        // 7. Verify repository files written with package updated
        Path repoDir = packageDir.resolve("repository");
        assertTrue(Files.exists(repoDir));
        Path studentRepoFile = repoDir.resolve("StudentRepository.java");
        assertTrue(Files.exists(studentRepoFile));
        String repoContent = Files.readString(studentRepoFile);
        assertTrue(repoContent.contains("package com.example.studentsystem.repository;"));
        assertTrue(repoContent.contains("import com.example.studentsystem.model.Student;"));

        // 8. Verify service files written with package updated
        Path serviceDir = packageDir.resolve("service");
        assertTrue(Files.exists(serviceDir));
        Path studentServiceFile = serviceDir.resolve("StudentService.java");
        assertTrue(Files.exists(studentServiceFile));
        String serviceContent = Files.readString(studentServiceFile);
        assertTrue(serviceContent.contains("package com.example.studentsystem.service;"));

        // 9. Verify serviceImpl files written with package updated
        Path serviceImplDir = serviceDir.resolve("impl");
        assertTrue(Files.exists(serviceImplDir));
        Path studentServiceImplFile = serviceImplDir.resolve("StudentServiceImpl.java");
        assertTrue(Files.exists(studentServiceImplFile));
        String serviceImplContent = Files.readString(studentServiceImplFile);
        assertTrue(serviceImplContent.contains("package com.example.studentsystem.service.impl;"));
        assertTrue(serviceImplContent.contains("import com.example.studentsystem.service.StudentService;"));

        // 10. Verify controller files written with package updated
        Path controllerDir = packageDir.resolve("controller");
        assertTrue(Files.exists(controllerDir));
        Path studentControllerFile = controllerDir.resolve("StudentController.java");
        assertTrue(Files.exists(studentControllerFile));
        String controllerContent = Files.readString(studentControllerFile);
        assertTrue(controllerContent.contains("package com.example.studentsystem.controller;"));

        // 11. Verify DTO files written with package updated
        Path dtoDir = packageDir.resolve("dto");
        assertTrue(Files.exists(dtoDir));
        Path studentDtoFile = dtoDir.resolve("StudentDTO.java");
        assertTrue(Files.exists(studentDtoFile));
        String dtoContent = Files.readString(studentDtoFile);
        assertTrue(dtoContent.contains("package com.example.studentsystem.dto;"));

        // 12. Verify Mapper files written with package updated
        Path mapperDir = packageDir.resolve("mapper");
        assertTrue(Files.exists(mapperDir));
        Path studentMapperFile = mapperDir.resolve("StudentMapper.java");
        assertTrue(Files.exists(studentMapperFile));
        String mapperContent = Files.readString(studentMapperFile);
        assertTrue(mapperContent.contains("package com.example.studentsystem.mapper;"));

        // 13. Verify that the generated source exactly matches GeneratedProject content (modulo package adjustments)
        assertEquals(
                entity.content().replace("uk.ac.bham.codeclassroom", "com.example.studentsystem"),
                studentContent
        );

        // 14. Verify existing directories are handled correctly (running again does not fail)
        Path rebuildPath = builder.build(generatedProject, metadata, tempDir);
        assertNotNull(rebuildPath);
        assertTrue(Files.exists(rebuildPath));
    }

    @Test
    void testInvalidOutputPathThrowsProjectBuilderException() {
        GeneratedProject generatedProject = new GeneratedProject(List.of());
        ProjectMetadata metadata = new ProjectMetadata(
                "StudentSystem", "com.example", "student-system", "com.example.studentsystem", "21", "3.5.0"
        );

        // Use a definitely non-writable and invalid path (like a root folder /some-invalid-path-that-doesnt-exist or null)
        Path invalidPath = Paths.get("/root/nonexistent_system_directory_no_permission_ever");
        assertThrows(ProjectBuilderException.class, () -> builder.build(generatedProject, metadata, invalidPath));
    }

    @Test
    void testNullInputsThrowProjectBuilderException(@TempDir Path tempDir) {
        GeneratedProject generatedProject = new GeneratedProject(List.of());
        ProjectMetadata metadata = new ProjectMetadata(
                "StudentSystem", "com.example", "student-system", "com.example.studentsystem", "21", "3.5.0"
        );

        assertThrows(ProjectBuilderException.class, () -> builder.build(null, metadata, tempDir));
        assertThrows(ProjectBuilderException.class, () -> builder.build(generatedProject, null, tempDir));
        assertThrows(ProjectBuilderException.class, () -> builder.build(generatedProject, metadata, null));
    }
}
