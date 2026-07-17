package uk.ac.bham.codeclassroom.generator.jdl;

import uk.ac.bham.codeclassroom.generator.engine.TemplateRenderer;

/**
 * Serializer that converts an ExtendedJDLDocument into its standard-compliant JDL format string
 * with custom CodeClassroom inheritance extensions using Mustache templates.
 */
public class JDLSerializer {

    private final TemplateRenderer templateRenderer = new TemplateRenderer();

    /**
     * Converts an ExtendedJDLDocument into a JDL formatted string.
     * Uses Mustache templates.
     *
     * @param document the ExtendedJDLDocument to serialize
     * @return the serialized JDL string
     */
    public String serialize(ExtendedJDLDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("ExtendedJDLDocument cannot be null");
        }

        // Render template using TemplateRenderer from uk.ac.bham.codeclassroom.generator.engine
        String rendered = templateRenderer.render("templates/jdl/extended-jdl.mustache", document);

        // Return standard trimmed format with single trailing newline
        return rendered.trim() + "\n";
    }
}
