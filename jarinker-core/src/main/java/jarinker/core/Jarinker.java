package jarinker.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

/**
 * @author Freeman
 * @since 2024/10/5
 */
public final class Jarinker {

    /**
     * Shrink the JAR files by removing unused classes.
     *
     * @param sourcePaths       The paths of the sources
     * @param dependenciesPaths The paths of the dependencies
     */
    public static void shrinkJars(List<Path> sourcePaths, List<Path> dependenciesPaths) {
        assert sourcePaths != null && dependenciesPaths != null;

        if (sourcePaths.isEmpty()) {
            throw new IllegalArgumentException("Source JAR files cannot be empty.");
        }
        if (dependenciesPaths.isEmpty()) {
            return;
        }

        // 第一步：解析依赖并分析使用的类
        var result = analyzeDependencies(sourcePaths, dependenciesPaths);

        // 第二步：基于分析结果生成精简后的 JAR 文件
        generateShrunkJars(result.dependencyJarToByteCodes, result.allUsedClasses);
    }

    private static final class DependencyAnalysisResult {
        Map<JarFile, LinkedHashSet<ByteCode>> dependencyJarToByteCodes;
        LinkedHashSet<ByteCode> allUsedClasses;

        DependencyAnalysisResult(
                Map<JarFile, LinkedHashSet<ByteCode>> dependencyJarToByteCodes,
                LinkedHashSet<ByteCode> allUsedClasses) {
            this.dependencyJarToByteCodes = dependencyJarToByteCodes;
            this.allUsedClasses = allUsedClasses;
        }
    }

    /**
     * Scan all classes in the given paths.
     *
     * @param paths The paths to scan
     * @return The scan result
     */
    public static ScanResult scan(List<Path> paths) {

        var classes = new LinkedHashSet<ByteCode>();
        var allClasses = new LinkedHashSet<ByteCode>();
        var dependencies = new LinkedHashMap<JarFile, LinkedHashSet<ByteCode>>();

        for (var path : paths) {
            var classList = ByteCodeUtil.listByteCodeInPath(path);
            classes.addAll(classList);
            allClasses.addAll(classList);
            for (var jar : ByteCodeUtil.listJarInPath(path)) {
                var clazzList = ByteCodeUtil.listByteCodeInJar(jar);
                dependencies.put(jar, clazzList);
                allClasses.addAll(clazzList);
            }
        }

        return ScanResult.of(classes, dependencies, allClasses);
    }

    private static DependencyAnalysisResult analyzeDependencies(List<Path> sourcePaths, List<Path> dependenciesPaths) {

        var sources = new LinkedHashSet<ByteCode>(512);
        var dependencies = new LinkedHashSet<ByteCode>(2048);
        var dependencyJarToByteCodes = new HashMap<JarFile, LinkedHashSet<ByteCode>>(128);

        for (var path : sourcePaths) {
            sources.addAll(ByteCodeUtil.listByteCodeInPath(path));
            for (var jar : ByteCodeUtil.listJarInPath(path)) {
                sources.addAll(ByteCodeUtil.listByteCodeInJar(jar));
            }
        }

        for (var path : dependenciesPaths) {
            for (var jar : ByteCodeUtil.listJarInPath(path)) {
                var byteCodes = ByteCodeUtil.listByteCodeInJar(jar);
                dependencies.addAll(byteCodes);
                dependencyJarToByteCodes.put(jar, byteCodes);
            }
        }

        var classpath = Classpath.of(dependencies);

        var allUsedClasses = new LinkedHashSet<ByteCode>(2048);

        for (var source : sources) {
            allUsedClasses.addAll(ByteCodeUtil.listUsedClassesInClasspath(source, classpath));
        }

        return new DependencyAnalysisResult(dependencyJarToByteCodes, allUsedClasses);
    }

    private static void generateShrunkJars(
            Map<JarFile, LinkedHashSet<ByteCode>> dependencyJarToByteCodes, LinkedHashSet<ByteCode> allUsedClasses) {

        var usedClasses = allUsedClasses.stream().map(ByteCode::getClassName).collect(Collectors.toSet());

        // 遍历每个依赖 Jar，生成精简的 JAR 文件
        for (var entry : dependencyJarToByteCodes.entrySet()) {
            var byteCodes = entry.getValue();
            var newByteCodes = byteCodes.stream()
                    .filter(byteCode -> usedClasses.contains(byteCode.getClassName()))
                    .toList();

            if (newByteCodes.isEmpty()) {
                continue; // 如果没有需要保留的类，跳过
            }

            var jar = entry.getKey();
            var newJarName = getShrunkJarName(jar.getName());

            try (var jarOutputStream = new JarOutputStream(new FileOutputStream(newJarName))) {
                for (var byteCode : newByteCodes) {
                    var jarEntry = new JarEntry(byteCode.getClassName().replace(".", "/") + ".class");
                    jarOutputStream.putNextEntry(jarEntry);
                    jarOutputStream.write(byteCode.getByteCode());
                    jarOutputStream.closeEntry();
                }

                var jarEntries = jar.entries();
                byte[] buffer = new byte[8192];
                while (jarEntries.hasMoreElements()) {
                    var jarEntry = jarEntries.nextElement();
                    if (!jarEntry.getName().endsWith(".class")) {
                        jarOutputStream.putNextEntry(jarEntry);
                        try (var is = jar.getInputStream(jarEntry)) {
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                jarOutputStream.write(buffer, 0, bytesRead);
                            }
                        }
                        jarOutputStream.closeEntry();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to write to the new jar file: " + newJarName, e);
            }
        }
    }

    /**
     * Generate the name for the shrunk JAR file by appending a suffix.
     *
     * @param originalName The original name of the JAR file.
     * @return The new name for the shrunk JAR file.
     */
    private static String getShrunkJarName(String originalName) {
        int lastDotIndex = originalName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return originalName.substring(0, lastDotIndex) + "-shrunk" + originalName.substring(lastDotIndex);
        } else {
            return originalName + "-shrunk";
        }
    }
}
