package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.Test;
import uk.ac.bham.codeclassroom.generator.api.ProjectGenerationOptions;
import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectGenerationOptionsTest {

    @Test
    void testUserProjectOptionsNormalizeToJHipsterConfiguration() {
        ProjectGenerationOptions options = new ProjectGenerationOptions(
            "Course Planner",
            "course-planner",
            "Com.Acme.Course Planner",
            "17",
            "mysql",
            "session",
            "gradle"
        );

        JHipsterProjectConfiguration config = options.toJHipsterConfiguration();

        assertEquals("coursePlanner", config.applicationName());
        assertEquals("com.acme.courseplanner", config.basePackage());
        assertEquals("17", config.javaVersion());
        assertEquals("mysql", config.databaseType());
        assertEquals("session", config.authenticationType());
        assertEquals("gradle", config.buildTool());
    }
}
