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
import org.jspecify.annotations.Nullable;

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
    public static @Nullable ClassInfo readClass(Path classFile) {
        if (!Files.exists(classFile)
                || !Files.isRegularFile(classFile)
                || !classFile.toString().endsWith(".class")) {
            return null;
        }

        try {
            byte[] bytecode = Files.readAllBytes(classFile);
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (IOException | RuntimeException e) {
            // Return null if analysis fails (invalid bytecode, parsing errors, etc.)
            return null;
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
                String className = classEntry.asInternalName().replace('/', '.');
                // Filter out primitive array types and the class itself
                if (!className.startsWith("[") && !className.equals(classInfo.getClassName())) {
                    dependencies.add(className);
                }
            }
        }

        // 2. Extract superclass dependency
        if (classModel.superclass().isPresent()) {
            String superClass = classModel.superclass().get().asInternalName().replace('/', '.');
            dependencies.add(superClass);
        }

        // 3. Extract interface dependencies
        for (var interfaceEntry : classModel.interfaces()) {
            dependencies.add(interfaceEntry.asInternalName().replace('/', '.'));
        }

        // 4. Extract field type dependencies
        for (FieldModel field : classModel.fields()) {
            extractTypesFromDescriptor(field.fieldTypeSymbol().descriptorString(), dependencies);
        }

        // 5. Extract method dependencies
        for (MethodModel method : classModel.methods()) {
            // Method parameters and return type
            extractTypesFromDescriptor(method.methodTypeSymbol().descriptorString(), dependencies);

            // Method body dependencies
            for (MethodElement element : method) {
                if (element instanceof CodeModel codeModel) {
                    extractCodeDependencies(codeModel, dependencies);
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
     * Extract class types from field/method descriptor.
     *
     * @param descriptor the descriptor string
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
            } else if (c == '[') {
                // Array type: skip array markers
                i++;
            } else {
                // Primitive type or other: skip
                i++;
            }
        }
    }

    /**
     * Extract dependencies from code model (method body).
     *
     * @param codeModel the code model
     * @param dependencies set to add dependencies to
     */
    private static void extractCodeDependencies(CodeModel codeModel, Set<String> dependencies) {
        for (var element : codeModel) {
            switch (element) {
                case InvokeInstruction invoke -> {
                    // Method invocation - add owner class
                    String ownerClass = invoke.owner().asInternalName().replace('/', '.');
                    dependencies.add(ownerClass);
                }
                case FieldInstruction field -> {
                    // Field access - add owner class
                    String ownerClass = field.owner().asInternalName().replace('/', '.');
                    dependencies.add(ownerClass);
                }
                case NewObjectInstruction newObj -> {
                    // Object creation - add class
                    String className = newObj.className().asInternalName().replace('/', '.');
                    dependencies.add(className);
                }
                case NewReferenceArrayInstruction newArray -> {
                    // Array creation - add component type
                    String componentType =
                            newArray.componentType().asInternalName().replace('/', '.');
                    dependencies.add(componentType);
                }
                case TypeCheckInstruction typeCheck -> {
                    // Type checking (instanceof, checkcast) - add class
                    String className = typeCheck.type().asInternalName().replace('/', '.');
                    dependencies.add(className);
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
     * @param classModel the class model
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

    public static String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
}
