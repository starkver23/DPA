package uk.ac.bham.codeclassroom.generator.jhipster.template;

import uk.ac.bham.codeclassroom.generator.jhipster.JHipsterProjectConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Scope context passed to Mustache templates for rendering JHipster artifacts.
 *
 * @param className                     the entity name
 * @param instanceName                  the camel-case variable name of the entity
 * @param pluralLowerName              the plural lower-case variable name of the entity
 * @param parentClassName               the direct parent entity class name
 * @param isRoot                        true if this entity is a root entity in the inheritance tree
 * @param requiresInheritanceAnnotation true if this entity requires the @Inheritance annotation
 * @param requiresExtends               true if this entity extends another class
 * @param basePackage                   the base Java package name for JHipster (e.g. com.mycompany.myapp)
 * @param fields                        local fields on the entity (mapped name/type)
 * @param inheritedFields               inherited fields from all ancestor entities (mapped name/type)
 * @param relationships                 associated relationships mapped to JHipster-compliant structures
 * @param configuration                 the general configuration for the JHipster application
 */
public record TemplateGenerationContext(
    String className,
    String instanceName,
    String pluralLowerName,
    String parentClassName,
    boolean isRoot,
    boolean requiresInheritanceAnnotation,
    boolean requiresExtends,
    String basePackage,
    List<Map<String, String>> fields,
    List<Map<String, String>> inheritedFields,
    List<Map<String, Object>> relationships,
    JHipsterProjectConfiguration configuration
) {}
