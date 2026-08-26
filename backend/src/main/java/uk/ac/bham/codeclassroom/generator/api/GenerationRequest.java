package uk.ac.bham.codeclassroom.generator.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Representation of a project code generation request.
 *
 * @param cdl                    the CDL source code to parse and generate
 * @param applicationName        optional generated application name
 * @param repositoryName         optional download/repository name
 * @param defaultJavaPackageName optional Java package name
 * @param javaVersion            optional Java version
 * @param databaseType           optional database type
 * @param authenticationType     optional authentication type
 * @param buildTool              optional build tool
 */
public record GenerationRequest(
    @NotBlank(message = "CDL source code must not be null or blank")
    String cdl,
    String applicationName,
    String repositoryName,
    String defaultJavaPackageName,
    String javaVersion,
    String databaseType,
    String authenticationType,
    String buildTool
) {}
