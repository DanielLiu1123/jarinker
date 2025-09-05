package jarinker.core;

import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Utility class for analyzing bytecode and extracting dependencies.
 *
 * @author Freeman
 * @since 2024/10/7
 */
public final class ByteCodeUtil {

    private ByteCodeUtil() {
        throw new UnsupportedOperationException("Util class cannot be instantiated");
    }

    /**
     * Analyze a single class file and return ClassInfo.
     *
     * @param classFile path to class file
     * @return ClassInfo or null if analysis fails
     */
    public static ClassInfo readClass(Path classFile) {
        if (!Files.exists(classFile)
                || !Files.isRegularFile(classFile)
                || !classFile.toString().endsWith(".class")) {
            throw new RuntimeException("Failed to analyze class: " + classFile);
        }

        try {
            byte[] bytecode = Files.readAllBytes(classFile);
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze class: " + classFile, e);
        }
    }

    /**
     * Analyze all classes in a JAR file.
     *
     * @param jarPath path to JAR file
     * @return map of class name to ClassInfo
     */
    public static Map<String, ClassInfo> readJar(Path jarPath) {
        if (!Files.exists(jarPath)
                || !Files.isRegularFile(jarPath)
                || !jarPath.toString().endsWith(".jar")) {
            throw new RuntimeException("Failed to analyze JAR: " + jarPath);
        }

        Map<String, ClassInfo> classes = new HashMap<>();

        try (var jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream().filter(entry -> entry.getName().endsWith(".class")).forEach(entry -> {
                try {
                    byte[] bytecode = jarFile.getInputStream(entry).readAllBytes();
                    ClassModel classModel = ClassFile.of().parse(bytecode);
                    ClassInfo classInfo = ClassInfo.of(classModel);
                    classes.put(classInfo.getClassName(), classInfo);
                } catch (IOException e) {
                    // Skip problematic classes
                    System.err.println("Failed to analyze class in JAR: " + entry.getName() + " - " + e.getMessage());
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze JAR: " + jarPath, e);
        }

        return classes;
    }

    /**
     * Extract dependencies from ClassInfo.
     *
     * @param classInfo class information
     * @return set of dependency class names
     */
    public static Set<String> extractDependencies(ClassInfo classInfo) {
        Set<String> dependencies = new HashSet<>();
        ClassModel classModel = classInfo.getModel();

        // 1. Extract all class references from constant pool
        for (var entry : classModel.constantPool()) {
            if (entry instanceof ClassEntry classEntry) {
                String internalName = classEntry.asInternalName();
                String className = normalizeClassName(internalName);
                // Filter out invalid class names and self-reference
                if (!className.isEmpty() && !className.equals(classInfo.getClassName())) {
                    dependencies.add(className);
                }
            }
        }

        // 2. Extract superclass dependency
        if (classModel.superclass().isPresent()) {
            String superClass = normalizeClassName(classModel.superclass().get().asInternalName());
            if (!superClass.isEmpty()) {
                dependencies.add(superClass);
            }
        }

        // 3. Extract interface dependencies
        for (var interfaceEntry : classModel.interfaces()) {
            String interfaceName = normalizeClassName(interfaceEntry.asInternalName());
            if (!interfaceName.isEmpty()) {
                dependencies.add(interfaceName);
            }
        }

        // 4. Extract field type dependencies
        for (FieldModel field : classModel.fields()) {
            extractTypesFromDescriptor(field.fieldTypeSymbol().descriptorString(), dependencies);
            // Extract generic signature dependencies
            field.findAttribute(Attributes.signature()).ifPresent(attr -> {
                extractTypesFromSignature(attr.signature().stringValue(), dependencies);
            });
        }

        // 5. Extract method dependencies
        for (MethodModel method : classModel.methods()) {
            // Method parameters and return type
            extractTypesFromDescriptor(method.methodTypeSymbol().descriptorString(), dependencies);

            // Extract generic signature dependencies
            method.findAttribute(Attributes.signature()).ifPresent(attr -> {
                extractTypesFromSignature(attr.signature().stringValue(), dependencies);
            });

            // Method body dependencies
            for (MethodElement element : method) {
                if (element instanceof CodeModel codeModel) {
                    extractCodeDependencies(codeModel, dependencies);
                    // Extract local variable type information
                    extractLocalVariableTypes(codeModel, dependencies);
                }
            }
        }

        // 6. Extract annotation dependencies
        extractAnnotationDependencies(classModel, dependencies);

        // Remove primitive types and array markers
        dependencies.removeIf(
                className -> className.isEmpty()
                        || className.length() == 1
                        || // primitive types
                        className.equals("void")
                        || className.equals(classInfo.getClassName()) // self-reference
                );

        return dependencies;
    }

    /**
     * Normalize JVM internal class name to standard Java class name.
     *
     * @param internalName JVM internal class name (e.g., "Ljava/lang/Object;", "[Ljava/lang/String;")
     * @return normalized class name or empty string if invalid/should be filtered
     */
    private static String normalizeClassName(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return "";
        }

        // Skip array types
        if (internalName.startsWith("[")) {
            return "";
        }

        // Handle object types: Ljava/lang/Object; -> java.lang.Object
        if (internalName.startsWith("L") && internalName.endsWith(";")) {
            return internalName.substring(1, internalName.length() - 1).replace('/', '.');
        }

        // Handle regular class names: java/lang/Object -> java.lang.Object
        if (!internalName.contains(";") && !internalName.startsWith("L")) {
            return internalName.replace('/', '.');
        }

        // Filter out primitive types and invalid formats
        return "";
    }

    /**
     * Extract class types from generic signature.
     * Generic signatures contain more detailed type information including generic parameters.
     *
     * @param signature    the generic signature string
     * @param dependencies set to add dependencies to
     */
    private static void extractTypesFromSignature(String signature, Set<String> dependencies) {
        if (signature == null || signature.isEmpty()) {
            return;
        }

        // Parse generic signature - this is a simplified parser
        // Generic signatures have format like: Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;
        int i = 0;
        while (i < signature.length()) {
            char c = signature.charAt(i);
            if (c == 'L') {
                // Object type: Lcom/example/Class; or Lcom/example/Class<...>;
                int end = findClassNameEnd(signature, i);
                if (end != -1) {
                    String internalName = signature.substring(i + 1, end);
                    String normalizedName = normalizeClassName("L" + internalName + ";");
                    if (!normalizedName.isEmpty()) {
                        dependencies.add(normalizedName);
                    }
                    i = end + 1;
                } else {
                    i++;
                }
            } else if (c == 'T') {
                // Type variable: TT; - skip type variables
                int end = signature.indexOf(';', i);
                if (end != -1) {
                    i = end + 1;
                } else {
                    i++;
                }
            } else {
                // Other characters: skip
                i++;
            }
        }
    }

    /**
     * Find the end of a class name in a generic signature, handling generic parameters.
     *
     * @param signature the signature string
     * @param start the start position (should be at 'L')
     * @return the position of the semicolon that ends the class name, or -1 if not found
     */
    private static int findClassNameEnd(String signature, int start) {
        int i = start + 1; // skip 'L'
        int depth = 0;

        while (i < signature.length()) {
            char c = signature.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ';' && depth == 0) {
                return i;
            }
            i++;
        }

        return -1;
    }

    /**
     * Extract class types from field/method descriptor.
     *
     * @param descriptor   the descriptor string
     * @param dependencies set to add dependencies to
     */
    private static void extractTypesFromDescriptor(String descriptor, Set<String> dependencies) {
        int i = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                // Object type: Lcom/example/Class;
                int end = descriptor.indexOf(';', i);
                if (end != -1) {
                    String className = descriptor.substring(i + 1, end).replace('/', '.');
                    dependencies.add(className);
                    i = end + 1;
                } else {
                    i++;
                }
            } else {
                // Primitive type or other: skip
                i++;
            }
        }
    }

    /**
     * Extract dependencies from local variable type information.
     *
     * @param codeModel    the code model
     * @param dependencies set to add dependencies to
     */
    private static void extractLocalVariableTypes(CodeModel codeModel, Set<String> dependencies) {
        // Extract from LocalVariableTable attribute
        codeModel.findAttribute(Attributes.localVariableTable()).ifPresent(attr -> {
            for (var localVar : attr.localVariables()) {
                extractTypesFromDescriptor(localVar.typeSymbol().descriptorString(), dependencies);
            }
        });

        // Extract from LocalVariableTypeTable attribute (for generic types)
        codeModel.findAttribute(Attributes.localVariableTypeTable()).ifPresent(attr -> {
            for (var localVar : attr.localVariableTypes()) {
                extractTypesFromSignature(localVar.signature().stringValue(), dependencies);
            }
        });
    }

    /**
     * Extract dependencies from code model (method body).
     *
     * @param codeModel    the code model
     * @param dependencies set to add dependencies to
     */
    private static void extractCodeDependencies(CodeModel codeModel, Set<String> dependencies) {
        for (var element : codeModel) {
            switch (element) {
                case InvokeInstruction invoke -> {
                    // Method invocation - add owner class
                    String ownerClass = normalizeClassName(invoke.owner().asInternalName());
                    if (!ownerClass.isEmpty()) {
                        dependencies.add(ownerClass);
                    }
                }
                case FieldInstruction field -> {
                    // Field access - add owner class
                    String ownerClass = normalizeClassName(field.owner().asInternalName());
                    if (!ownerClass.isEmpty()) {
                        dependencies.add(ownerClass);
                    }
                }
                case NewObjectInstruction newObj -> {
                    // Object creation - add class
                    String className = normalizeClassName(newObj.className().asInternalName());
                    if (!className.isEmpty()) {
                        dependencies.add(className);
                    }
                }
                case NewReferenceArrayInstruction newArray -> {
                    // Array creation - add component type
                    String componentType =
                            normalizeClassName(newArray.componentType().asInternalName());
                    if (!componentType.isEmpty()) {
                        dependencies.add(componentType);
                    }
                }
                case TypeCheckInstruction typeCheck -> {
                    // Type checking (instanceof, checkcast) - add class
                    String className = normalizeClassName(typeCheck.type().asInternalName());
                    if (!className.isEmpty()) {
                        dependencies.add(className);
                    }
                }
                case InvokeDynamicInstruction invokeDynamic -> {
                    // Dynamic invocation - extract from bootstrap method
                    // The actual dependencies are complex and may require deeper analysis
                    // For now, we rely on constant pool extraction
                }
                default -> {
                    // Other instructions - dependencies should be captured by constant pool
                }
            }
        }
    }

    /**
     * Extract annotation dependencies from class model.
     *
     * @param classModel   the class model
     * @param dependencies set to add dependencies to
     */
    private static void extractAnnotationDependencies(ClassModel classModel, Set<String> dependencies) {
        // Class-level annotations
        classModel.findAttribute(Attributes.runtimeVisibleAnnotations()).ifPresent(attr -> {
            for (var annotation : attr.annotations()) {
                String annotationClassName =
                        annotation.className().stringValue().replace('/', '.');
                dependencies.add(annotationClassName);
            }
        });

        classModel.findAttribute(Attributes.runtimeInvisibleAnnotations()).ifPresent(attr -> {
            for (var annotation : attr.annotations()) {
                String annotationClassName =
                        annotation.className().stringValue().replace('/', '.');
                dependencies.add(annotationClassName);
            }
        });

        // Field annotations
        for (FieldModel field : classModel.fields()) {
            field.findAttribute(Attributes.runtimeVisibleAnnotations()).ifPresent(attr -> {
                for (var annotation : attr.annotations()) {
                    String annotationClassName =
                            annotation.className().stringValue().replace('/', '.');
                    dependencies.add(annotationClassName);
                }
            });

            field.findAttribute(Attributes.runtimeInvisibleAnnotations()).ifPresent(attr -> {
                for (var annotation : attr.annotations()) {
                    String annotationClassName =
                            annotation.className().stringValue().replace('/', '.');
                    dependencies.add(annotationClassName);
                }
            });
        }

        // Method annotations
        for (MethodModel method : classModel.methods()) {
            method.findAttribute(Attributes.runtimeVisibleAnnotations()).ifPresent(attr -> {
                for (var annotation : attr.annotations()) {
                    String annotationClassName =
                            annotation.className().stringValue().replace('/', '.');
                    dependencies.add(annotationClassName);
                }
            });

            method.findAttribute(Attributes.runtimeInvisibleAnnotations()).ifPresent(attr -> {
                for (var annotation : attr.annotations()) {
                    String annotationClassName =
                            annotation.className().stringValue().replace('/', '.');
                    dependencies.add(annotationClassName);
                }
            });

            // Parameter annotations
            method.findAttribute(Attributes.runtimeVisibleParameterAnnotations())
                    .ifPresent(attr -> {
                        for (var paramAnnotations : attr.parameterAnnotations()) {
                            for (var annotation : paramAnnotations) {
                                String annotationClassName =
                                        annotation.className().stringValue().replace('/', '.');
                                dependencies.add(annotationClassName);
                            }
                        }
                    });

            method.findAttribute(Attributes.runtimeInvisibleParameterAnnotations())
                    .ifPresent(attr -> {
                        for (var paramAnnotations : attr.parameterAnnotations()) {
                            for (var annotation : paramAnnotations) {
                                String annotationClassName =
                                        annotation.className().stringValue().replace('/', '.');
                                dependencies.add(annotationClassName);
                            }
                        }
                    });
        }
    }
}
