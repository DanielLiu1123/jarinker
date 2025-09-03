package jarinker.core;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * File visitor for scanning class files and jar files.
 */
class ClassFileVisitor extends SimpleFileVisitor<Path> {
    private final Map<String, ClassInfo> classes;

    public ClassFileVisitor(Map<String, ClassInfo> classes) {
        this.classes = classes;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (attrs.isRegularFile()) {
            if (isJar(file)) {
                var jarClasses = ByteCodeUtil.analyzeJar(file);
                classes.putAll(jarClasses);
            } else if (isClass(file)) {
                var classInfo = ByteCodeUtil.analyzeClass(file);
                classes.put(classInfo.getClassName(), classInfo);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    private static boolean isJar(Path file) {
        return file.toString().endsWith(".jar");
    }

    private static boolean isClass(Path file) {
        return file.toString().endsWith(".class");
    }
}
