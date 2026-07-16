package uk.ac.bham.codeclassroom.generator.project;

/**
 * Generates the application.properties configuration file for the standalone Spring Boot project.
 */
public class ApplicationPropertiesGenerator {

    /**
     * Generates sensible application properties based on ProjectMetadata.
     *
     * @param metadata the project metadata configurations
     * @return the formatted properties string
     */
    public static String generate(ProjectMetadata metadata) {
        return """
                spring.application.name=%s
                server.port=8080

                # H2 Database configuration
                spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
                spring.datasource.driverClassName=org.h2.Driver
                spring.datasource.username=sa
                spring.datasource.password=
                spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

                # Hibernate/JPA Configuration
                spring.jpa.hibernate.ddl-auto=update
                spring.jpa.show-sql=true
                spring.jpa.properties.hibernate.format_sql=true

                # H2 Console Configuration
                spring.h2.console.enabled=true
                spring.h2.console.path=/h2-console
                spring.h2.console.settings.web-allow-others=true
                """.formatted(metadata.projectName());
    }
}
