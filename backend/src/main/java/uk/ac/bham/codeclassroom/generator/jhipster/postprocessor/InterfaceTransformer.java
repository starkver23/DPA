package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import uk.ac.bham.codeclassroom.generator.ast.EntityKind;
import uk.ac.bham.codeclassroom.generator.ast.EntityNode;
import uk.ac.bham.codeclassroom.generator.ast.MethodNode;
import uk.ac.bham.codeclassroom.generator.ast.ParameterNode;
import uk.ac.bham.codeclassroom.generator.ast.TypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post-processor stage to generate Java interface files and inject implements clauses
 * and method implementations into entity classes.
 */
public class InterfaceTransformer {

    /**
     * Generates Java interfaces and transforms entity classes.
     *
     * @param context      the transformation context
     * @param changedFiles list tracking all modified files
     * @throws TransformationException if file I/O or transformations fail
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        String basePkg = context.extendedJDLDocument().configuration().basePackage();
        Path domainPath = context.generatedProjectPath().resolve("src/main/java")
            .resolve(basePkg.replace('.', '/'))
            .resolve("domain");

        if (!Files.exists(domainPath)) {
            return;
        }

        // 1. Generate physical interface files
        for (EntityNode iface : context.extendedJDLDocument().interfaces()) {
            Path ifaceFile = domainPath.resolve(iface.name() + ".java");
            try {
                String content = generateInterfaceContent(basePkg, iface);
                Files.writeString(ifaceFile, content);
                changedFiles.add(ifaceFile);
            } catch (IOException e) {
                throw new TransformationException("Failed to write interface file: " + iface.name(), e);
            }
        }

        // 2. Build a map of name -> EntityNode for interfaces
        java.util.Map<String, EntityNode> interfaceMap = new java.util.HashMap<>();
        for (EntityNode iface : context.extendedJDLDocument().interfaces()) {
            interfaceMap.put(iface.name(), iface);
        }

        // 3. Inject implements clauses and methods
        for (EntityNode originalClass : context.extendedJDLDocument().originalEntities()) {
            if (originalClass.kind() == EntityKind.CLASS) {
                Path entityFile = domainPath.resolve(originalClass.name() + ".java");
                if (Files.exists(entityFile)) {
                    try {
                        String content = Files.readString(entityFile);

                        // Match class declaration with optional extends and existing implements clauses.
                        if (!originalClass.implementsInterfaces().isEmpty()) {
                            Pattern classPattern = Pattern.compile(
                                "public\\s+(abstract\\s+)?class\\s+" + originalClass.name() +
                                    "(\\s+extends\\s+\\w+)?(\\s+implements\\s+[^\\{]+)?"
                            );
                            Matcher matcher = classPattern.matcher(content);
                            if (matcher.find()) {
                                String declaration = matcher.group(0);
                                String interfacesList = String.join(", ", originalClass.implementsInterfaces());
                                String newDeclaration;
                                if (matcher.group(3) != null) {
                                    newDeclaration = declaration.trim().replaceFirst(
                                        "\\s+implements\\s+",
                                        " implements " + interfacesList + ", "
                                    ) + " ";
                                } else {
                                    newDeclaration = declaration.trim() + " implements " + interfacesList + " ";
                                }
                                content = content.replace(declaration, newDeclaration);
                            }
                        }

                        // Generate required interface method bodies for concrete classes
                        if (!originalClass.abstractClass()) {
                            java.util.Set<String> implementedIfaces = new java.util.HashSet<>();
                            for (String directImpl : originalClass.implementsInterfaces()) {
                                collectAllExtendedInterfaces(directImpl, interfaceMap, implementedIfaces);
                            }

                            StringBuilder methodsBuffer = new StringBuilder();
                            for (String ifaceName : implementedIfaces) {
                                EntityNode ifaceNode = interfaceMap.get(ifaceName);
                                if (ifaceNode != null) {
                                    for (MethodNode method : ifaceNode.methods()) {
                                        if (!content.contains(" " + method.name() + "(")) {
                                            methodsBuffer.append("\n");
                                            methodsBuffer.append("    @Override\n");
                                            String returnTypeStr = method.returnType().map(TypeReference::toString).orElse("void");
                                            methodsBuffer.append("    public ").append(returnTypeStr).append(" ").append(method.name()).append("(");
                                            for (int i = 0; i < method.parameters().size(); i++) {
                                                if (i > 0) {
                                                    methodsBuffer.append(", ");
                                                }
                                                ParameterNode param = method.parameters().get(i);
                                                methodsBuffer.append(param.type().toString()).append(" ").append(param.name());
                                            }
                                            methodsBuffer.append(") {\n");
                                            methodsBuffer.append(getDefaultReturnStatement(method.returnType())).append("\n");
                                            methodsBuffer.append("    }\n");
                                        }
                                    }
                                }
                            }

                            if (methodsBuffer.length() > 0) {
                                int lastBrace = content.lastIndexOf('}');
                                if (lastBrace != -1) {
                                    content = content.substring(0, lastBrace) + methodsBuffer.toString() + "\n" + content.substring(lastBrace);
                                }
                            }
                        }

                        // Inject explicitly declared CDL class methods
                        StringBuilder classMethodsBuffer = new StringBuilder();
                        for (MethodNode method : originalClass.methods()) {
                            if (!content.contains(" " + method.name() + "(")) {
                                classMethodsBuffer.append("\n");
                                String returnTypeStr = method.returnType().map(TypeReference::toString).orElse("void");
                                classMethodsBuffer.append("    public ").append(returnTypeStr).append(" ").append(method.name()).append("(");
                                for (int i = 0; i < method.parameters().size(); i++) {
                                    if (i > 0) {
                                        classMethodsBuffer.append(", ");
                                    }
                                    ParameterNode param = method.parameters().get(i);
                                    classMethodsBuffer.append(param.type().toString()).append(" ").append(param.name());
                                }
                                classMethodsBuffer.append(") {\n");
                                classMethodsBuffer.append(getDefaultReturnStatement(method.returnType())).append("\n");
                                classMethodsBuffer.append("    }\n");
                            }
                        }

                        if (classMethodsBuffer.length() > 0) {
                            int lastBrace = content.lastIndexOf('}');
                            if (lastBrace != -1) {
                                content = content.substring(0, lastBrace) + classMethodsBuffer.toString() + "\n" + content.substring(lastBrace);
                            }
                        }

                        Files.writeString(entityFile, content);
                        changedFiles.add(entityFile);
                    } catch (IOException e) {
                        throw new TransformationException("Failed to transform class entity: " + originalClass.name(), e);
                    }
                }
            }
        }
    }

    private String generateInterfaceContent(String basePkg, EntityNode iface) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePkg).append(".domain;\n\n");
        Set<String> imports = collectImports(iface);
        for (String importName : imports) {
            sb.append("import ").append(importName).append(";\n");
        }
        if (!imports.isEmpty()) {
            sb.append("\n");
        }

        sb.append("public interface ").append(iface.name());
        if (!iface.extendsInterfaces().isEmpty()) {
            sb.append(" extends ").append(String.join(", ", iface.extendsInterfaces()));
        }
        sb.append(" {\n");

        for (MethodNode method : iface.methods()) {
            sb.append(formatMethodSignature(method)).append("\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private Set<String> collectImports(EntityNode iface) {
        Set<String> imports = new LinkedHashSet<>();
        for (MethodNode method : iface.methods()) {
            method.returnType().ifPresent(type -> collectTypeImport(type, imports));
            for (ParameterNode parameter : method.parameters()) {
                collectTypeImport(parameter.type(), imports);
            }
        }
        return imports;
    }

    private void collectTypeImport(TypeReference type, Set<String> imports) {
        switch (type.baseType()) {
            case "List", "Set", "Map", "Collection" -> imports.add("java.util." + type.baseType());
            case "BigDecimal" -> imports.add("java.math.BigDecimal");
            case "LocalDate" -> imports.add("java.time.LocalDate");
            default -> { }
        }
        type.genericType().ifPresent(genericType -> collectTypeImport(genericType, imports));
    }

    private String formatMethodSignature(MethodNode method) {
        StringBuilder sb = new StringBuilder();
        String returnTypeStr = method.returnType().map(TypeReference::toString).orElse("void");
        sb.append("    ").append(returnTypeStr).append(" ").append(method.name()).append("(");
        for (int i = 0; i < method.parameters().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            ParameterNode param = method.parameters().get(i);
            sb.append(param.type().toString()).append(" ").append(param.name());
        }
        sb.append(");");
        return sb.toString();
    }

    private void collectAllExtendedInterfaces(String interfaceName, java.util.Map<String, EntityNode> interfaceMap, java.util.Set<String> result) {
        if (!result.add(interfaceName)) {
            return;
        }
        EntityNode node = interfaceMap.get(interfaceName);
        if (node != null) {
            for (String parent : node.extendsInterfaces()) {
                collectAllExtendedInterfaces(parent, interfaceMap, result);
            }
        }
    }

    private String getDefaultReturnStatement(Optional<TypeReference> returnType) {
        if (returnType.isEmpty()) {
            return "";
        }
        String typeStr = returnType.get().toString();
        switch (typeStr) {
            case "void": return "";
            case "String": return "        return \"\";";
            case "Integer": return "        return 0;";
            case "Long": return "        return 0L;";
            case "Double": return "        return 0.0;";
            case "Boolean": return "        return false;";
            case "BigDecimal": return "        return java.math.BigDecimal.ZERO;";
            default: return "        return null;";
        }
    }
}
