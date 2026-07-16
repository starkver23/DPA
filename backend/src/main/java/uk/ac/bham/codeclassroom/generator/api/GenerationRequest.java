package uk.ac.bham.codeclassroom.generator.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Representation of a project code generation request.
 *
 * @param cdl the CDL source code to parse and generate
 */
public record GenerationRequest(
    @NotBlank(message = "CDL source code must not be null or blank")
    String cdl
) {}
