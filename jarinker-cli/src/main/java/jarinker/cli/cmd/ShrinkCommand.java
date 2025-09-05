package jarinker.cli.cmd;

import jarinker.core.DependencyGraph;
import jarinker.core.JarShrinker;
import jarinker.core.JdepsAnalyzer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Shrink command for artifact shrinking.
 *
 * @author Freeman
 */
@CommandLine.Command(description = "Shrink artifacts by removing unused classes", mixinStandardHelpOptions = true)
public class ShrinkCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-cp", "-classpath", "--class-path"},
            description = "Classpath entries (can be specified multiple times)",
            required = true)
    private List<Path> classpath;

    @CommandLine.Parameters(description = "Source artifacts to shrink (JAR files or class directories)", arity = "1..*")
    private List<Path> sources;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Output directory for shrunk artifacts")
    private Path outputDir;

    @CommandLine.Option(
            names = {"--in-place"},
            description = "Shrink artifacts in place (default: true if no output directory specified)")
    private Boolean inPlace;

    @CommandLine.Option(
            names = {"--include-jdk"},
            description = "Include JDK classes in analysis (default: false)")
    private boolean includeJdk;

    @Override
    public Integer call() throws IOException {
        // Validate parameters
        validateParameters();

        // Step 1: Dependency analysis
        var jdepsAnalyzer = JdepsAnalyzer.builder().includeJdk(includeJdk).build();

        DependencyGraph graph = jdepsAnalyzer.analyze(sources, classpath);

        // Step 2: Extract reachable classes from dependency graph
        Map<String, Set<String>> reachableClasses = extractReachableClasses(graph);

        // Step 3: Execute shrink
        var shrinker = JarShrinker.builder().inPlace(isInPlace()).build();

        var result = shrinker.shrink(sources, reachableClasses, outputDir);

        // Print results
        printShrinkResult(result);

        return 0;
    }

    private void validateParameters() {
        if (outputDir != null && Boolean.TRUE.equals(inPlace)) {
            throw new IllegalArgumentException("Cannot specify both --output and --in-place");
        }
    }

    private boolean isInPlace() {
        return outputDir == null || Boolean.TRUE.equals(inPlace);
    }

    private Map<String, Set<String>> extractReachableClasses(DependencyGraph graph) {
        // Extract all classes from the dependency graph
        Map<String, Set<String>> reachableClasses = new HashMap<>();

        // For simplicity, consider all classes in the graph as reachable
        // In a real implementation, this would do proper reachability analysis
        Set<String> allClasses = graph.getNodeNames();

        // If no entry points specified, use all classes as entry points
        reachableClasses.put("*", allClasses);

        return reachableClasses;
    }

    private void printShrinkResult(JarShrinker.ShrinkResult result) {
        System.out.println("=== Shrink Results ===");
        System.out.println("Processed JARs: " + result.processedJars());
        System.out.println("Original size: " + formatBytes(result.originalSize()));
        System.out.println("Shrunk size: " + formatBytes(result.shrunkSize()));
        System.out.println("Size reduction: " + String.format("%.1f%%", result.getReductionPercentage()));
        System.out.println("Saved bytes: " + formatBytes(result.getSavedBytes()));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
