package jarinker.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

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
    public static ClassInfo analyzeClass(Path classFile) {
        try {
            byte[] bytecode = Files.readAllBytes(classFile);
            ClassReader reader = new ClassReader(bytecode);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);

            String className = classNode.name.replace('/', '.');
            String packageName = getPackageName(className);
            boolean isInterface = (classNode.access & org.objectweb.asm.Opcodes.ACC_INTERFACE) != 0;
            boolean isAbstract = (classNode.access & org.objectweb.asm.Opcodes.ACC_ABSTRACT) != 0;
            String superClass = classNode.superName != null ? classNode.superName.replace('/', '.') : null;

            HashSet<String> interfaces = new HashSet<>();
            if (classNode.interfaces != null) {
                for (String iface : classNode.interfaces) {
                    interfaces.add(iface.replace('/', '.'));
                }
            }

            HashSet<String> annotations = new HashSet<>();
            if (classNode.visibleAnnotations != null) {
                for (AnnotationNode annotation : classNode.visibleAnnotations) {
                    annotations.add(Type.getType(annotation.desc).getClassName());
                }
            }

            long size = bytecode.length;

            return new ClassInfo(
                    className,
                    packageName,
                    isInterface,
                    isAbstract,
                    superClass != null ? superClass : "java.lang.Object",
                    interfaces,
                    annotations,
                    size);

        } catch (Exception e) {
            // Return a dummy ClassInfo for invalid class files to avoid null
            Path fileName = classFile.getFileName();
            String fileNameStr = fileName != null ? fileName.toString() : "Unknown";
            String className =
                    fileNameStr.endsWith(".class") ? fileNameStr.substring(0, fileNameStr.length() - 6) : fileNameStr;
            return new ClassInfo(className, "", false, false, "java.lang.Object", new HashSet<>(), new HashSet<>(), 0);
        }
    }

    /**
     * Analyze all classes in a JAR file.
     *
     * @param jarPath path to JAR file
     * @return map of class name to ClassInfo
     */
    public static Map<String, ClassInfo> analyzeJar(Path jarPath) {
        Map<String, ClassInfo> classes = new HashMap<>();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream().filter(entry -> entry.getName().endsWith(".class")).forEach(entry -> {
                try {
                    byte[] bytecode = jarFile.getInputStream(entry).readAllBytes();
                    ClassReader reader = new ClassReader(bytecode);
                    ClassNode classNode = new ClassNode();
                    reader.accept(classNode, 0);

                    String className = classNode.name.replace('/', '.');
                    String packageName = getPackageName(className);
                    boolean isInterface = (classNode.access & org.objectweb.asm.Opcodes.ACC_INTERFACE) != 0;
                    boolean isAbstract = (classNode.access & org.objectweb.asm.Opcodes.ACC_ABSTRACT) != 0;
                    String superClass = classNode.superName != null ? classNode.superName.replace('/', '.') : null;

                    HashSet<String> interfaces = new HashSet<>();
                    if (classNode.interfaces != null) {
                        for (String iface : classNode.interfaces) {
                            interfaces.add(iface.replace('/', '.'));
                        }
                    }

                    HashSet<String> annotations = new HashSet<>();
                    if (classNode.visibleAnnotations != null) {
                        for (AnnotationNode annotation : classNode.visibleAnnotations) {
                            annotations.add(Type.getType(annotation.desc).getClassName());
                        }
                    }

                    long size = bytecode.length;

                    ClassInfo classInfo = new ClassInfo(
                            className,
                            packageName,
                            isInterface,
                            isAbstract,
                            superClass != null ? superClass : "java.lang.Object",
                            interfaces,
                            annotations,
                            size);
                    classes.put(className, classInfo);

                } catch (IOException e) {
                    // Skip problematic classes
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
        // For now, return a simple set based on superclass and interfaces
        Set<String> dependencies = new HashSet<>();

        if (classInfo.getSuperClass() != null) {
            dependencies.add(classInfo.getSuperClass());
        }

        dependencies.addAll(classInfo.getInterfaces());
        dependencies.addAll(classInfo.getAnnotations());

        return dependencies;
    }

    private static String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
}
