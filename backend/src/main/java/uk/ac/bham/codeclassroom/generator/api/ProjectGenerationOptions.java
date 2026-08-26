package uk.ac.bham.codeclassroom.generator.api;

import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;

/**
 * User-editable project settings used for full application generation.
 */
public record ProjectGenerationOptions(
    String applicationName,
    String repositoryName,
    String defaultJavaPackageName,
    String javaVersion,
    String databaseType,
    String authenticationType,
    String buildTool
) {
    public static ProjectGenerationOptions fromRequest(GenerationRequest request) {
        return new ProjectGenerationOptions(
            request.applicationName(),
            request.repositoryName(),
            request.defaultJavaPackageName(),
            request.javaVersion(),
            request.databaseType(),
            request.authenticationType(),
            request.buildTool()
        );
    }

    public JHipsterProjectConfiguration toJHipsterConfiguration() {
        return new JHipsterProjectConfiguration(
            normalizeApplicationName(applicationName),
            normalizePackageName(defaultJavaPackageName),
            valueOrDefault(javaVersion, "21"),
            "3.2.5",
            "8.2.1",
            valueOrDefault(databaseType, "postgresql"),
            valueOrDefault(authenticationType, "jwt"),
            valueOrDefault(buildTool, "maven"),
            "react"
        );
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String normalizeApplicationName(String value) {
        String fallback = "generatedApp";
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String[] parts = value.trim().split("[^A-Za-z0-9]+");
        StringBuilder normalized = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (normalized.isEmpty()) {
                normalized.append(part.substring(0, 1).toLowerCase()).append(part.substring(1));
            } else {
                normalized.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }

        if (normalized.isEmpty() || !Character.isLetter(normalized.charAt(0))) {
            return fallback;
        }
        return normalized.toString();
    }

    private static String normalizePackageName(String value) {
        String fallback = "com.mycompany.codeclassroom";
        if (value == null || value.isBlank()) {
            return fallback;
        }

        StringBuilder packageName = new StringBuilder();
        for (String segment : value.trim().toLowerCase().split("\\.")) {
            String cleaned = segment.replaceAll("[^a-z0-9_]", "");
            if (cleaned.isBlank() || !Character.isLetter(cleaned.charAt(0))) {
                continue;
            }
            if (!packageName.isEmpty()) {
                packageName.append(".");
            }
            packageName.append(cleaned);
        }
        return packageName.isEmpty() ? fallback : packageName.toString();
    }
}
