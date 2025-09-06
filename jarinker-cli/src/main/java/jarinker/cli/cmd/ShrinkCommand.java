package jarinker.cli.cmd;

import com.sun.tools.jdeps.JdepsFilter;
import jarinker.core.AnalyzerType;
import jarinker.core.DependencyGraph;
import jarinker.core.JarShrinker;
import jarinker.core.JdepsAnalyzer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Shrink command for artifact shrinking.
 *
 * @author Freeman
 */
@Command(description = "Shrink artifacts by removing unused classes", mixinStandardHelpOptions = true)
public class ShrinkCommand implements Runnable {

    @Parameters(description = "Source artifacts to shrink (JAR files or class directories)", arity = "1..*")
    private List<Path> sources;

    @Option(
            names = {"-cp", "-classpath", "--class-path"},
            description = "Classpath entries (can be specified multiple times)",
            required = true)
    private List<Path> classpath;

    @Option(
            names = {"-o", "--output"},
            description = "Output directory for shrunk artifacts")
    private @Nullable Path outputDir;

    // === jdeps options ===

    // Filter options
    @Option(
            names = {"--filter-pattern"},
            description = "Filter dependencies matching the given pattern")
    private @Nullable Pattern filterPattern;

    @Option(
            names = {"--regex"},
            description = "Find dependencies matching the given pattern")
    private @Nullable Pattern regex;

    @Option(
            names = {"--filter-same-package"},
            defaultValue = "false",
            description = "Filter dependencies within the same package")
    private Boolean filterSamePackage;

    @Option(
            names = {"--filter-same-archive"},
            defaultValue = "false",
            description = "Filter dependencies within the same archive")
    private Boolean filterSameArchive;

    @Option(
            names = {"--find-jdk-internals"},
            defaultValue = "false",
            description = "Find class-level dependencies on JDK internal APIs")
    private Boolean findJDKInternals;

    @Option(
            names = {"--find-missing-deps"},
            defaultValue = "false",
            description = "Find missing dependencies")
    private Boolean findMissingDeps;

    // Source filters
    @Option(
            names = {"--include-pattern"},
            description = "Restrict analysis to classes matching pattern")
    private @Nullable Pattern includePattern;

    @Option(
            names = {"--requires"},
            defaultValue = "[]",
            description = "Find dependencies matching the given module name (can be specified multiple times)")
    private @Nullable List<String> requires;

    @Option(
            names = {"--target-packages"},
            defaultValue = "[]",
            description = "Find dependencies matching the given package name (can be specified multiple times)")
    private @Nullable List<String> targetPackages;

    // === shrink options end ===

    @Override
    @SneakyThrows
    public void run() {

        // Step 1: Analyze dependencies
        DependencyGraph graph;

        try (var jdepsConfiguration = JdepsAnalyzer.buildJdepsConfiguration(sources, classpath, Runtime.version())) {
            var analyzer = JdepsAnalyzer.builder()
                    .jdepsFilter(buildJdepsFilter())
                    .jdepsConfiguration(jdepsConfiguration)
                    .type(AnalyzerType.CLASS)
                    .build();

            graph = analyzer.analyze();
        }

        // Step 2: Extract reachable classes from dependency graph
        Map<String, Set<String>> reachableClasses = extractReachableClasses(graph);

        // Step 3: Execute shrink
        var shrinker = JarShrinker.builder().outputDir(outputDir).build();

        var result = shrinker.shrink(getDependentJars(classpath), reachableClasses);

        // Print results
        printShrinkResult(result);
    }

    private List<Path> getDependentJars(List<Path> classpath) {
        return classpath.stream()
                .filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                .toList();
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

    /**
     * Build JdepsFilterBuilder with all configured options.
     *
     * @return configured JdepsFilterBuilder
     */
    private JdepsFilter buildJdepsFilter() {
        var filterBuilder = new JdepsFilter.Builder();

        if (regex != null) {
            filterBuilder.regex(regex);
        }

        filterBuilder.filter(filterSamePackage, filterSameArchive);

        if (filterPattern != null) {
            filterBuilder.filter(filterPattern);
        }

        filterBuilder.findJDKInternals(findJDKInternals);

        filterBuilder.findMissingDeps(findMissingDeps);

        if (includePattern != null) {
            filterBuilder.includePattern(includePattern);
        }

        if (requires != null) {
            Set<String> pkgs = targetPackages != null ? Set.copyOf(targetPackages) : Set.of();
            for (String require : requires) {
                filterBuilder.requires(require, pkgs);
            }
        }

        return filterBuilder.build();
    }
}
