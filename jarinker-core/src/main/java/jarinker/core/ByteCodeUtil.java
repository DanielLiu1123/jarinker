package jarinker.core;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.JarFile;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

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
     * Get direct dependencies of a class, do NOT include transitive dependencies.
     *
     * @param byteCode bytecode
     * @return direct dependencies of the class, in format of "java.lang.String"
     */
    public static LinkedHashSet<String> getDependencies(ByteCode byteCode) {
        assert byteCode != null;
        return getDependencies(getClassNode(byteCode));
    }

    /**
     * Get all bytecodes in a path, including class files and class files in jar files; this method will recursively walk through the path.
     *
     * @param path path
     * @return All bytecodes in the path
     */
    public static List<LibJar> listLibJarInPath(Path path) {
        assert path != null;

        var libJars = new ArrayList<LibJar>();

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var filename = file.toString();
                    if (filename.endsWith(".jar")) {
                        try (var jarFile = new JarFile(file.toFile())) {
                            libJars.add(LibJar.of(jarFile));
                        }
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return libJars;
    }

    /**
     * Get all jars in a path, this method will recursively walk through the path.
     *
     * @param path path
     * @return All jars in the path
     */
    public static List<JarFile> listJarInPath(Path path) {
        assert path != null;

        var jars = new ArrayList<JarFile>();

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var filename = file.toString();
                    if (filename.endsWith(".jar")) {
                        var jarFile = new JarFile(file.toFile());
                        jars.add(jarFile);
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jars;
    }

    /**
     * List all .class files in a path, this method will recursively walk through the path.
     *
     * @param path path
     * @return All bytecodes in the path
     */
    public static List<ByteCode> listByteCodeInPath(Path path) {
        var byteCodes = new ArrayList<ByteCode>();

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var filename = file.toString();
                    if (filename.endsWith(".class")) {
                        byteCodes.add(ByteCode.of(Files.readAllBytes(file)));
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteCodes;
    }

    /**
     * Get all bytecodes in a jar file.
     *
     * @param jarFile jar file
     * @return All bytecodes in the jar file
     */
    public static LinkedHashSet<ByteCode> listByteCodeInJar(JarFile jarFile) {
        var byteCodes = new LinkedHashSet<ByteCode>();

        var entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                try {
                    var is = jarFile.getInputStream(entry);
                    byteCodes.add(ByteCode.of(is.readAllBytes()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return byteCodes;
    }

    /**
     * Get all dependencies in a jar file, including transitive dependencies.
     *
     * @param jarFile jar file
     * @return All dependencies in the jar file, including transitive dependencies.
     */
    public static LinkedHashSet<ByteCode> listDependenciesInJar(JarFile jarFile, ArrayList<JarFile> libs) {
        assert jarFile != null && libs != null;

        //        libs.remove(jarFile);
        //
        //        var byteCodes = listByteCodeInJar(jarFile);
        //        var result = new LinkedHashSet<ByteCode>();
        //
        //        var directDependencies = listByteCodeInJar(jarFile);
        //        result.addAll(directDependencies);
        //
        //        for (var dep : directDependencies) {
        //            var transitiveDependencies = getDependencies(dep);

        return new LinkedHashSet<>(0);
    }

    /**
     * Get all used classes for the bytecode in the classpath, including transitive dependencies.
     *
     * @param byteCode  bytecode
     * @param classpath classpath
     * @return All used classes for the bytecode in the classpath, including transitive dependencies.
     */
    public static LinkedHashSet<ByteCode> listUsedClassesInClasspath(ByteCode byteCode, Classpath classpath) {
        var usedClasses = new LinkedHashSet<ByteCode>();
        var visited = new HashSet<String>();
        var stack = new ArrayDeque<Dep>();

        stack.push(Dep.of(byteCode, classpath));

        while (!stack.isEmpty()) {
            var currentDep = stack.pop();
            var currentByteCode = currentDep.byteCode();
            var className = currentByteCode.className();

            if (!visited.add(className)) {
                continue;
            }

            usedClasses.add(currentByteCode);

            for (var externalDep : currentDep.getExternalDependencies()) {
                if (!visited.contains(externalDep.className())) {
                    stack.push(Dep.of(externalDep, classpath));
                }
            }
        }

        usedClasses.remove(byteCode); // Remove self

        return usedClasses;
    }

    private static ClassNode getClassNode(ByteCode byteCode) {
        ClassReader classReader = new ClassReader(byteCode.byteCode());
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    private static LinkedHashSet<String> getDependencies(ClassNode classNode) {
        var dependencies = new LinkedHashSet<String>();

        // Add the superclass and interfaces as dependencies
        if (classNode.superName != null) {
            addDependency(dependencies, classNode.superName);
        }
        for (String interfaceName : classNode.interfaces) {
            addDependency(dependencies, interfaceName);
        }

        // Analyze fields and methods for dependencies
        collectFieldDependencies(classNode, dependencies);
        collectMethodDependencies(classNode, dependencies);

        // Analyze class annotations for dependencies
        collectAnnotationsDependencies(classNode.visibleAnnotations, dependencies);
        collectAnnotationsDependencies(classNode.invisibleAnnotations, dependencies);

        dependencies.remove(className(classNode.name)); // Remove self

        return dependencies;
    }

    private static void collectFieldDependencies(ClassNode classNode, LinkedHashSet<String> dependencies) {
        for (FieldNode field : classNode.fields) {
            addDependency(dependencies, field.desc);
            collectAnnotationsDependencies(field.visibleAnnotations, dependencies);
            collectAnnotationsDependencies(field.invisibleAnnotations, dependencies);
        }
    }

    private static void collectMethodDependencies(ClassNode classNode, LinkedHashSet<String> dependencies) {
        for (MethodNode method : classNode.methods) {

            // Method signature dependencies (return type, argument types, exceptions)
            addMethodSignatureDependencies(method.desc, dependencies);

            for (String exception : method.exceptions) {
                addDependency(dependencies, exception);
            }

            // Analyze method instructions to find method calls and type references
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof TypeInsnNode typeInsn) {
                    // TypeInsnNode covers instructions like NEW, ANEWARRAY, CHECKCAST, INSTANCEOF
                    if (isClassDescriptor(typeInsn.desc)) {
                        addDependency(dependencies, typeInsn.desc);
                    } else if (typeInsn.desc.contains("/")) {
                        addDependency(dependencies, typeInsn.desc);
                    }
                } else if (insn instanceof MethodInsnNode methodInsn) {
                    // MethodInsnNode represents method invocations
                    addDependency(dependencies, methodInsn.owner);
                }
            }

            // Analyze method annotations
            collectAnnotationsDependencies(method.visibleAnnotations, dependencies);
            collectAnnotationsDependencies(method.invisibleAnnotations, dependencies);

            // Analyze parameter annotations
            if (method.visibleParameterAnnotations != null) {
                for (var paramAnnotations : method.visibleParameterAnnotations) {
                    collectAnnotationsDependencies(paramAnnotations, dependencies);
                }
            }
            if (method.invisibleParameterAnnotations != null) {
                for (var paramAnnotations : method.invisibleParameterAnnotations) {
                    collectAnnotationsDependencies(paramAnnotations, dependencies);
                }
            }
        }
    }

    private static void collectAnnotationsDependencies(
            List<AnnotationNode> annotations, LinkedHashSet<String> dependencies) {

        for (AnnotationNode annotation : annotations) {
            addDependency(dependencies, annotation.desc);

            if (annotation.values != null) {
                for (Object value : annotation.values) {
                    if (value instanceof String desc && isClassDescriptor(desc)) {
                        addDependency(dependencies, desc);
                    }
                }
            }
        }
    }

    private static void addMethodSignatureDependencies(String descriptor, LinkedHashSet<String> dependencies) {
        Type method = Type.getMethodType(descriptor);

        addDependency(dependencies, method.getReturnType().getDescriptor());

        for (Type argumentType : method.getArgumentTypes()) {
            addDependency(dependencies, argumentType.getDescriptor());
        }
    }

    private static void addDependency(LinkedHashSet<String> dependencies, String classname) {
        var clazz = className(classname);
        if (clazz != null) {
            dependencies.add(clazz);
        }
    }

    private static boolean isClassDescriptor(String descriptor) {
        if (descriptor == null) {
            return false;
        }

        while (descriptor.startsWith("[")) {
            descriptor = descriptor.substring(1);
        }

        return descriptor.startsWith("L") && descriptor.endsWith(";");
    }

    /**
     * Normalize class name from various formats.
     *
     * @param classname class name in the format of "java/lang/String" or "java.lang.String" or "Ljava/lang/String;" or [Ljava.lang.String;"
     * @return class name in the format of "java.lang.String", null if the input is invalid. e.g., primitive types or method descriptors
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.3">Descriptors</a>
     */
    @Nullable
    private static String className(String classname) {

        assert classname != null && !classname.isBlank() : "Class name cannot be empty";

        // Skip array descriptors
        while (classname.startsWith("[")) {
            classname = classname.substring(1);
        }

        // Skip primitive types and method descriptors
        if (classname.length() == 1 || classname.contains("(")) {
            return null;
        }

        // Class descriptors "L<classname>;"
        if (classname.startsWith("L") && classname.endsWith(";")) {
            classname = classname.substring(1, classname.length() - 1).replace('/', '.');
        }

        if (classname.contains("/")) {
            classname = classname.replace('/', '.');
        }

        return classname;
    }
}
