package jarinker.core;

import com.sun.tools.jdeps.Archive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;

/**
 * Shrinks JAR files by removing unused classes.
 * This is a simplified implementation that will be enhanced with actual JAR processing.
 *
 * @author Freeman
 */
@Builder
public class JarShrinker {

    private @Nullable Path outputDir;

    /**
     * Shrink JAR files based on reachable classes.
     *
     * @param archives archives to shrink
     * @return shrink result
     */
    @SneakyThrows
    public ShrinkResult shrink(List<Archive> archives, DependencyGraph graph) {

        long originalSize = 0;
        long shrunkSize = 0;
        int processedJars = 0;

        Set<String> allReachableClasses = graph.getDependenciesMap().values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        for (var archive : archives) {
            var path = archive.path().orElse(null);
            if (path == null) {
                continue;
            }

            if (!Files.isRegularFile(path) || !path.toString().endsWith(".jar")) {
                continue;
            }

            long jarOriginalSize = Files.size(path);
            originalSize += jarOriginalSize;

            Path outputPath;
            if (outputDir == null) {
                // no output dir, do it in place
                Path parent = path.getParent();
                if (parent == null) {
                    parent = Path.of(".");
                }
                outputPath = parent.resolve(path.getFileName() + ".tmp");
            } else {
                if (!Files.exists(outputDir)) {
                    Files.createDirectories(outputDir);
                }
                outputPath = outputDir.resolve(path.getFileName());
            }

            // Shrink the JAR
            shrinkJar(path, outputPath, allReachableClasses);

            // If in-place, replace the original file
            if (outputDir == null) {
                Files.move(outputPath, path, StandardCopyOption.REPLACE_EXISTING);
                outputPath = path;
            }

            long jarShrunkSize = Files.size(outputPath);
            shrunkSize += jarShrunkSize;
            processedJars++;
        }

        return new ShrinkResult(processedJars, originalSize, shrunkSize);
    }

    @SneakyThrows
    private static void shrinkJar(Path inputJar, Path outputJar, Set<String> reachableClasses) {
        try (var inputStream = Files.newInputStream(inputJar);
                var jarInput = new JarInputStream(inputStream);
                var outputStream = Files.newOutputStream(outputJar);
                var jarOutput = new JarOutputStream(outputStream)) {

            // Copy manifest if present
            var manifest = jarInput.getManifest();
            if (manifest != null) {
                jarOutput.putNextEntry(new java.util.jar.JarEntry("META-INF/MANIFEST.MF"));
                manifest.write(jarOutput);
                jarOutput.closeEntry();
            }

            java.util.jar.JarEntry entry;
            while ((entry = jarInput.getNextJarEntry()) != null) {
                String entryName = entry.getName();

                // Skip directories and manifest (already handled)
                if (entry.isDirectory() || entryName.equals("META-INF/MANIFEST.MF")) {
                    continue;
                }

                // Skip signature files
                if (entryName.startsWith("META-INF/")
                        && (entryName.endsWith(".SF") || entryName.endsWith(".DSA") || entryName.endsWith(".RSA"))) {
                    continue;
                }

                // For class files, check if they are reachable
                if (entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').replaceAll("\\.class$", "");

                    // Keep the class if it's in the reachable set or if we have no reachability info
                    if (reachableClasses.isEmpty()
                            || reachableClasses.contains(className)
                            || reachableClasses.stream()
                                    .anyMatch(reachable ->
                                            className.startsWith(reachable) || reachable.startsWith(className))) {
                        copyEntry(jarInput, jarOutput, entry);
                    }
                } else {
                    // Keep all non-class files (resources, etc.)
                    copyEntry(jarInput, jarOutput, entry);
                }
            }
        }
    }

    private static void copyEntry(JarInputStream input, JarOutputStream output, JarEntry entry) throws IOException {
        // Create new entry to avoid issues with compressed entries
        var newEntry = new JarEntry(entry.getName());
        newEntry.setTime(entry.getTime());

        output.putNextEntry(newEntry);

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }

        output.closeEntry();
    }

    /**
     * Result of shrinking operation.
     */
    public record ShrinkResult(int processedJars, long originalSize, long shrunkSize) {

        public double getReductionPercentage() {
            if (originalSize == 0) return 0.0;
            return ((double) (originalSize - shrunkSize) / originalSize) * 100.0;
        }

        public long getSavedBytes() {
            return originalSize - shrunkSize;
        }
    }
}
