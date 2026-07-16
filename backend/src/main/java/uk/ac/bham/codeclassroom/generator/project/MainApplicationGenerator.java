package uk.ac.bham.codeclassroom.generator.project;

/**
 * Generates the main Spring Boot @SpringBootApplication bootstrap entry class.
 */
public class MainApplicationGenerator {

    /**
     * Generates a valid GeneratedApplication.java source code based on ProjectMetadata.
     *
     * @param metadata the project metadata configurations
     * @return the formatted Java source string
     */
    public static String generate(ProjectMetadata metadata) {
        return """
                package %s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class GeneratedApplication {
                    public static void main(String[] args) {
                        SpringApplication.run(GeneratedApplication.class, args);
                    }
                }
                """.formatted(metadata.packageName());
    }
}
