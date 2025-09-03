package jarinker.core;

import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
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
            throw new IllegalArgumentException("Invalid class file: " + classFile);
        }

        try {
            byte[] bytecode = Files.readAllBytes(classFile);
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read class file: " + classFile, e);
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
            throw new IllegalArgumentException("Invalid jar file: " + jarPath);
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

        // Add superclass dependency
        if (classModel.superclass().isPresent()) {
            String superClass = classModel.superclass().get().asInternalName().replace('/', '.');
            dependencies.add(superClass);
        }

        // Add interface dependencies
        for (var interfaceEntry : classModel.interfaces()) {
            dependencies.add(interfaceEntry.asInternalName().replace('/', '.'));
        }

        // Add annotation dependencies
        classModel.findAttribute(Attributes.runtimeVisibleAnnotations()).ifPresent(attr -> {
            for (var annotation : attr.annotations()) {
                String annotationClassName =
                        annotation.className().stringValue().replace('/', '.');
                dependencies.add(annotationClassName);
            }
        });

        return dependencies;
    }

    public static String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
}
