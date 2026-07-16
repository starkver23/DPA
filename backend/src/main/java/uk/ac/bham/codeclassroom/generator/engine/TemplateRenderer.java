package uk.ac.bham.codeclassroom.generator.engine;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Compiles and renders Mustache templates from classpath resources.
 */
public class TemplateRenderer {

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    /**
     * Renders a classpath template using the provided data scope.
     *
     * @param templatePath classpath resource path (e.g. "templates/Entity.mustache")
     * @param scope        context map or object to resolve placeholders
     * @return the fully rendered string content
     */
    public String render(String templatePath, Object scope) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);
            Objects.requireNonNull(is, "Template not found on classpath: " + templatePath);
            
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Mustache mustache = mustacheFactory.compile(reader, templatePath);
                StringWriter writer = new StringWriter();
                mustache.execute(writer, scope);
                return writer.toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to render template: " + templatePath, e);
        }
    }
}
