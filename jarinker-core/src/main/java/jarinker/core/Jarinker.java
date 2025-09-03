package jarinker.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Main class for JAR shrinking operations.
 * Provides analyze() and shrink() methods according to the new API design.
 *
 * @author Freeman
 */
public final class Jarinker {

    private final List<Path> sources;
    private final List<Path> dependencies;
    private final JarinkerConfig config;

    /**
     * Package-private constructor for JarinkerBuilder.
     *
     * @param sources      source paths
     * @param dependencies dependency paths
     * @param config       configuration
     */
    Jarinker(List<Path> sources, List<Path> dependencies, JarinkerConfig config) {
        this.sources = new ArrayList<>(sources);
        this.dependencies = new ArrayList<>(dependencies);
        this.config = config;
    }

    /**
     * Execute dependency analysis.
     *
     * @return analysis result
     * @throws RuntimeException if analysis fails
     */
    public AnalysisResult analyze() {
        long startTime = System.currentTimeMillis();

        try {
            if (config.isVerbose()) {
                System.out.println("Starting dependency analysis...");
                System.out.println("Sources: " + sources);
                System.out.println("Dependencies: " + dependencies);
            }

            // Scan all classes from sources and dependencies
            Map<String, ClassInfo> allClasses = new HashMap<>();
            Set<String> entryPoints = new HashSet<>();

            // Scan source classes (these are entry points)
            for (Path sourcePath : sources) {
                var sourceClasses = getClasses(sourcePath);
                allClasses.putAll(sourceClasses);
                entryPoints.addAll(sourceClasses.keySet());

                if (config.isVerbose()) {
                    System.out.println("Scanned source: " + sourcePath + " (" + sourceClasses.size() + " classes)");
                }
            }

            // Scan dependency classes
            for (Path dependencyPath : dependencies) {
                var dependencyClasses = getClasses(dependencyPath);
                allClasses.putAll(dependencyClasses);

                if (config.isVerbose()) {
                    System.out.println(
                            "Scanned dependency: " + dependencyPath + " (" + dependencyClasses.size() + " classes)");
                }
            }

            // Build dependency graph
            Map<String, Set<String>> dependencyGraph = buildDependencyGraph(allClasses);

            // Apply include/exclude patterns
            allClasses = applyPatterns(allClasses);

            // Generate statistics
            long endTime = System.currentTimeMillis();
            Duration analysisTime = Duration.ofMillis(endTime - startTime);

            Map<String, Integer> packageCounts = calculatePackageCounts(allClasses);
            int usedCount = calculateUsedClassCount(allClasses, dependencyGraph, entryPoints);

            AnalysisStatistics statistics = new AnalysisStatistics(
                    allClasses.size(), usedCount, allClasses.size() - usedCount, analysisTime, packageCounts);

            List<AnalysisWarning> warnings = new ArrayList<>();

            if (config.isVerbose()) {
                System.out.println("Analysis completed in " + analysisTime.toMillis() + "ms");
                System.out.println("Total classes: " + allClasses.size());
                System.out.println("Used classes: " + usedCount);
                System.out.println("Unused classes: " + (allClasses.size() - usedCount));
            }

            return new AnalysisResult(allClasses, dependencyGraph, entryPoints, warnings, statistics);

        } catch (Exception e) {
            throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute JAR shrinking.
     *
     * @return shrink result
     * @throws RuntimeException if shrinking fails
     */
    public ShrinkResult shrink() {
        long startTime = System.currentTimeMillis();

        try {
            if (config.isVerbose()) {
                System.out.println("Starting JAR shrinking...");
            }

            // First analyze dependencies
            AnalysisResult analysis = analyze();

            // Apply shrink strategy to determine required classes
            Set<String> requiredClasses = config.getStrategy().determineRequiredClasses(analysis);

            if (config.isVerbose()) {
                System.out.println("Required classes: " + requiredClasses.size());
            }

            // Process each dependency JAR
            List<ShrunkJar> shrunkJars = new ArrayList<>();
            long totalSizeBefore = 0;
            long totalSizeAfter = 0;
            int totalClassesBefore = 0;
            int totalClassesAfter = 0;

            for (Path dependencyPath : dependencies) {
                if (Files.isRegularFile(dependencyPath)
                        && dependencyPath.toString().endsWith(".jar")) {
                    ShrunkJar shrunkJar = processJar(dependencyPath, requiredClasses);
                    if (shrunkJar != null) {
                        shrunkJars.add(shrunkJar);
                        totalSizeBefore += shrunkJar.getOriginalSize();
                        totalSizeAfter += shrunkJar.getShrunkSize();
                        totalClassesBefore += shrunkJar.getOriginalClassCount();
                        totalClassesAfter += shrunkJar.getShrunkClassCount();
                    }
                }
            }

            // Generate statistics
            long endTime = System.currentTimeMillis();
            Duration processingTime = Duration.ofMillis(endTime - startTime);

            Map<String, Long> jarSizeReductions = new HashMap<>();
            for (ShrunkJar jar : shrunkJars) {
                jarSizeReductions.put(jar.getOriginalName(), jar.getSizeSaved());
            }

            ShrinkStatistics statistics = new ShrinkStatistics(
                    totalSizeBefore,
                    totalSizeAfter,
                    totalClassesBefore,
                    totalClassesAfter,
                    processingTime,
                    jarSizeReductions);

            List<ShrinkWarning> warnings = new ArrayList<>();
            Optional<ShrinkReport> report = generateReport(shrunkJars, statistics, requiredClasses);

            if (config.isVerbose()) {
                System.out.println("Shrinking completed in " + processingTime.toMillis() + "ms");
                System.out.println("Total size before: " + totalSizeBefore + " bytes");
                System.out.println("Total size after: " + totalSizeAfter + " bytes");
                System.out.println("Size saved: " + (totalSizeBefore - totalSizeAfter) + " bytes");
                System.out.printf("Shrink ratio: %.2f%%%n", statistics.getShrinkRatio() * 100);
            }

            return new ShrinkResult(shrunkJars, statistics, warnings, report);

        } catch (Exception e) {
            throw new RuntimeException("Shrinking failed: " + e.getMessage(), e);
        }
    }

    // Helper methods

    private static Map<String, ClassInfo> getClasses(Path path) {
        Map<String, ClassInfo> classes = new HashMap<>();

        if (Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new ClassFileVisitor(classes));
            } catch (IOException e) {
                throw new RuntimeException("Failed to scan classes in " + path, e);
            }
        } else if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
            classes.putAll(ByteCodeUtil.readJar(path));
        } else if (Files.isRegularFile(path) && path.toString().endsWith(".class")) {
            ClassInfo classInfo = ByteCodeUtil.readClass(path);
            classes.put(classInfo.getClassName(), classInfo);
        }

        return classes;
    }

    private Map<String, Set<String>> buildDependencyGraph(Map<String, ClassInfo> allClasses) {
        Map<String, Set<String>> dependencyGraph = new HashMap<>();

        for (ClassInfo classInfo : allClasses.values()) {
            Set<String> dependencies = ByteCodeUtil.extractDependencies(classInfo);
            dependencyGraph.put(classInfo.getClassName(), dependencies);
        }

        return dependencyGraph;
    }

    private Map<String, ClassInfo> applyPatterns(Map<String, ClassInfo> allClasses) {
        Map<String, ClassInfo> filteredClasses = new HashMap<>();

        for (Map.Entry<String, ClassInfo> entry : allClasses.entrySet()) {
            String className = entry.getKey();

            // Check include patterns
            boolean included = config.getIncludePatterns().isEmpty();
            for (String pattern : config.getIncludePatterns()) {
                if (matchesPattern(className, pattern)) {
                    included = true;
                    break;
                }
            }

            // Check exclude patterns
            boolean excluded = false;
            for (String pattern : config.getExcludePatterns()) {
                if (matchesPattern(className, pattern)) {
                    excluded = true;
                    break;
                }
            }

            if (included && !excluded) {
                filteredClasses.put(className, entry.getValue());
            }
        }

        return filteredClasses;
    }

    private boolean matchesPattern(String className, String pattern) {
        // Simple pattern matching with * wildcard support
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return className.matches(regex);
    }

    private Map<String, Integer> calculatePackageCounts(Map<String, ClassInfo> allClasses) {
        Map<String, Integer> packageCounts = new HashMap<>();

        for (ClassInfo classInfo : allClasses.values()) {
            String className = classInfo.getClassName();
            String packageName = ByteCodeUtil.getPackageName(className);
            packageCounts.put(packageName, packageCounts.getOrDefault(packageName, 0) + 1);
        }

        return packageCounts;
    }

    private int calculateUsedClassCount(
            Map<String, ClassInfo> allClasses, Map<String, Set<String>> dependencyGraph, Set<String> entryPoints) {
        Set<String> usedClasses = new HashSet<>();
        Set<String> visited = new HashSet<>();

        // Start from entry points and collect all reachable classes
        for (String entryPoint : entryPoints) {
            collectUsedClasses(entryPoint, dependencyGraph, usedClasses, visited);
        }

        return usedClasses.size();
    }

    private void collectUsedClasses(
            String className, Map<String, Set<String>> dependencyGraph, Set<String> usedClasses, Set<String> visited) {
        if (!visited.add(className)) {
            return; // Already visited
        }

        usedClasses.add(className);

        Set<String> dependencies = dependencyGraph.get(className);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                collectUsedClasses(dependency, dependencyGraph, usedClasses, visited);
            }
        }
    }

    private ShrunkJar processJar(Path jarPath, Set<String> requiredClasses) {
        try {
            Path fileName = jarPath.getFileName();
            String originalName = fileName != null ? fileName.toString() : "unknown.jar";
            String shrunkName = getShrunkJarName(originalName);

            // Calculate original JAR statistics
            long originalSize = Files.size(jarPath);
            int originalClassCount = countClassesInJar(jarPath);

            // Determine output path
            Path parentPath = jarPath.getParent();
            if (parentPath == null) {
                parentPath = jarPath.toAbsolutePath().getParent();
                if (parentPath == null) {
                    parentPath = jarPath.getFileSystem().getPath(".");
                }
            }
            Path outputPath = config.getOutputDirectory().orElse(parentPath).resolve(shrunkName);

            // Create the shrunk JAR
            int shrunkClassCount = createShrunkJar(jarPath, outputPath, requiredClasses);
            long shrunkSize = Files.exists(outputPath) ? Files.size(outputPath) : 0;

            if (config.isVerbose()) {
                System.out.println("Created shrunk JAR: " + outputPath);
                System.out.println("  Original: " + originalClassCount + " classes, " + formatSize(originalSize));
                System.out.println("  Shrunk: " + shrunkClassCount + " classes, " + formatSize(shrunkSize));
            }

            return new ShrunkJar(
                    originalName,
                    shrunkName,
                    originalSize,
                    shrunkSize,
                    originalClassCount,
                    shrunkClassCount,
                    outputPath);

        } catch (IOException e) {
            if (config.isVerbose()) {
                System.err.println("Failed to process JAR: " + jarPath + " - " + e.getMessage());
            }
            // Return a dummy ShrunkJar to avoid null
            Path fileName = jarPath.getFileName();
            String originalName = fileName != null ? fileName.toString() : "unknown.jar";
            String shrunkName = getShrunkJarName(originalName);
            return new ShrunkJar(originalName, shrunkName, 0, 0, 0, 0, jarPath);
        }
    }

    private Optional<ShrinkReport> generateReport(
            List<ShrunkJar> shrunkJars, ShrinkStatistics statistics, Set<String> requiredClasses) {
        LocalDateTime timestamp = LocalDateTime.now();
        String summary = String.format(
                "Shrunk %d JAR files, saved %d bytes (%.2f%% reduction)",
                shrunkJars.size(), statistics.getSizeSaved(), statistics.getShrinkRatio() * 100);

        List<String> removedClasses = new ArrayList<>();
        List<String> keptClasses = new ArrayList<>(requiredClasses);

        return Optional.of(new ShrinkReport(timestamp, summary, removedClasses, keptClasses, statistics));
    }

    /**
     * Generate the name for the shrunk JAR file by appending a suffix.
     *
     * @param originalName The original name of the JAR file.
     * @return The new name for the shrunk JAR file.
     */
    private String getShrunkJarName(String originalName) {
        int lastDotIndex = originalName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return originalName.substring(0, lastDotIndex) + "-shrunk" + originalName.substring(lastDotIndex);
        } else {
            return originalName + "-shrunk";
        }
    }

    /**
     * Count the number of class files in a JAR
     */
    private int countClassesInJar(Path jarPath) throws IOException {
        int count = 0;
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Create a shrunk JAR containing only the required classes
     */
    private int createShrunkJar(Path originalJar, Path outputJar, Set<String> requiredClasses) throws IOException {
        // Ensure output directory exists
        Path outputDir = outputJar.getParent();
        if (outputDir != null) {
            Files.createDirectories(outputDir);
        }

        int includedClassCount = 0;

        try (JarFile inputJar = new JarFile(originalJar.toFile());
                JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(outputJar))) {

            var entries = inputJar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String entryName = entry.getName();

                boolean shouldInclude = false;

                if (entry.isDirectory()) {
                    // Always include directories
                    shouldInclude = true;
                } else if (entryName.endsWith(".class")) {
                    // Include class files that are required
                    String className = entryName.replace('/', '.').replace(".class", "");
                    shouldInclude = requiredClasses.contains(className);

                    // Debug output to help troubleshoot
                    if (config.isVerbose() && entryName.contains("Strings")) {
                        System.out.println("Checking class: " + className + " -> " + shouldInclude);
                    }

                    if (shouldInclude) {
                        includedClassCount++;
                    }
                } else {
                    // Include non-class files (resources, META-INF, etc.)
                    shouldInclude = true;
                }

                if (shouldInclude) {
                    // Create new entry (avoid duplicate entry exception)
                    JarEntry newEntry = new JarEntry(entryName);
                    newEntry.setTime(entry.getTime());

                    try {
                        outputStream.putNextEntry(newEntry);

                        if (!entry.isDirectory()) {
                            // Copy file content
                            try (InputStream inputStream = inputJar.getInputStream(entry)) {
                                inputStream.transferTo(outputStream);
                            }
                        }

                        outputStream.closeEntry();
                    } catch (IOException e) {
                        if (config.isVerbose()) {
                            System.err.println("Warning: Failed to copy entry " + entryName + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        return includedClassCount;
    }

    /**
     * Format file size in human readable format
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
