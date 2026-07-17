package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import uk.ac.bham.codeclassroom.generator.jdl.JDLInheritance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Post-processor stage to update generated JHipster entity TestSample classes
 * to use setter-based initialization instead of chained builders. This fixes compilation
 * issues caused by builder return type mismatches in inheritance hierarchies.
 */
public class TestSampleTransformer {

    /**
     * Applies transformation to test sample classes for entities participating in inheritance.
     *
     * @param context      the transformation context
     * @param changedFiles the list tracking all modified files
     * @throws TransformationException if file operations fail
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        Path testDomainPath = context.generatedProjectPath().resolve("src/test/java")
            .resolve(context.extendedJDLDocument().configuration().basePackage().replace('.', '/'))
            .resolve("domain");

        if (!Files.exists(testDomainPath)) {
            return;
        }

        for (JDLInheritance inh : context.extendedJDLDocument().inheritanceDeclarations()) {
            Path testSampleFile = testDomainPath.resolve(inh.childEntity() + "TestSamples.java");
            if (Files.exists(testSampleFile)) {
                try {
                    String content = Files.readString(testSampleFile);
                    String transformed = transformTestSamples(content, inh.childEntity());
                    if (!transformed.equals(content)) {
                        Files.writeString(testSampleFile, transformed);
                        changedFiles.add(testSampleFile);
                    }
                } catch (IOException e) {
                    throw new TransformationException("Failed to transform TestSamples for entity: " + inh.childEntity(), e);
                }
            }
        }
    }

    private String transformTestSamples(String content, String entityName) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("return\\s+new\\s+" + entityName + "\\(\\)([^;]*);");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String chain = matcher.group(1);
            List<String> methods = parseMethods(chain);
            
            String varName = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
            
            StringBuilder replacement = new StringBuilder();
            replacement.append("        ").append(entityName).append(" ").append(varName).append(" = new ").append(entityName).append("();\n");
            for (String m : methods) {
                replacement.append("        ").append(toSetter(varName, m)).append("\n");
            }
            replacement.append("        return ").append(varName).append(";");
            
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<String> parseMethods(String chain) {
        List<String> methods = new java.util.ArrayList<>();
        if (chain == null || chain.isBlank()) {
            return methods;
        }
        int i = 0;
        while (i < chain.length()) {
            if (chain.charAt(i) == '.') {
                i++; // skip '.'
                // Read method name
                StringBuilder methodName = new StringBuilder();
                while (i < chain.length() && Character.isJavaIdentifierPart(chain.charAt(i))) {
                    methodName.append(chain.charAt(i));
                    i++;
                }
                
                // Expect '('
                if (i < chain.length() && chain.charAt(i) == '(') {
                    i++; // skip '('
                    // Read arguments, accounting for nested parentheses
                    StringBuilder args = new StringBuilder();
                    int parenCount = 1;
                    while (i < chain.length() && parenCount > 0) {
                        char c = chain.charAt(i);
                        if (c == '(') {
                            parenCount++;
                        } else if (c == ')') {
                            parenCount--;
                        }
                        if (parenCount > 0) {
                            args.append(c);
                        }
                        i++;
                    }
                    methods.add(methodName.toString() + "(" + args.toString() + ")");
                }
            } else {
                i++;
            }
        }
        return methods;
    }

    private String toSetter(String varName, String methodCall) {
        int parenIdx = methodCall.indexOf('(');
        if (parenIdx == -1) {
            return "";
        }
        String name = methodCall.substring(0, parenIdx);
        String args = methodCall.substring(parenIdx); // e.g. "(1L)"
        
        String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return varName + ".set" + capitalized + args + ";";
    }
}
