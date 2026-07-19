package uk.ac.bham.codeclassroom.generator.jhipster;

/**
 * Immutable configuration metadata for the future generated JHipster application.
 *
 * @param applicationName    the name of the JHipster application
 * @param basePackage        the base Java package
 * @param javaVersion        the Java version to target
 * @param springBootVersion  the Spring Boot version to target
 * @param jhipsterVersion    the JHipster version to target
 * @param databaseType       the type of database (e.g., postgresql, mysql, mongodb)
 * @param authenticationType the authentication mechanism (e.g., jwt, oauth2, session)
 * @param buildTool          the build tool (e.g., maven, gradle)
 * @param frontendFramework  the frontend framework (e.g., react, angular, vue)
 */
public record JHipsterProjectConfiguration(
    String applicationName,
    String basePackage,
    String javaVersion,
    String springBootVersion,
    String jhipsterVersion,
    String databaseType,
    String authenticationType,
    String buildTool,
    String frontendFramework
) {
    /**
     * Creates a configuration with default JHipster settings.
     *
     * @param applicationName the name of the application
     * @return a configuration with default values
     */
    public static JHipsterProjectConfiguration createDefault(String applicationName) {
        return new JHipsterProjectConfiguration(
            applicationName,
            "com.mycompany.codeclassroom",
            "21",
            "3.2.5",
            "8.2.1",
            "postgresql",
            "jwt",
            "maven",
            "react"
        );
    }
}
