package uk.ac.bham.codeclassroom.generator.project;

/**
 * Metadata configuration driving the standalone Spring Boot project generation.
 *
 * @param projectName      the simple name of the project
 * @param groupId          the Maven group ID (e.g. "com.example")
 * @param artifactId       the Maven artifact ID (e.g. "demo-app")
 * @param packageName      the base package name (e.g. "com.example.demoapp")
 * @param javaVersion      the Java compiler and runtime version (e.g. "21")
 * @param springBootVersion the Spring Boot framework version (e.g. "3.5.0")
 */
public record ProjectMetadata(
    String projectName,
    String groupId,
    String artifactId,
    String packageName,
    String javaVersion,
    String springBootVersion
) {}
