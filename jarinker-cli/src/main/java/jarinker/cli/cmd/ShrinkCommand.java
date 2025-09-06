package jarinker.cli.cmd;

import com.sun.tools.jdeps.JdepsFilter;
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
import java.util.regex.Pattern;
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
public class ShrinkCommand implements Callable<Integer> {

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
            defaultValue = "true",
            description = "Filter dependencies within the same package")
    private Boolean filterSamePackage;

    @Option(
            names = {"--filter-same-archive"},
            defaultValue = "true",
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
    private List<String> requires;

    @Option(
            names = {"--target-packages"},
            defaultValue = "[]",
            description = "Find dependencies matching the given package name (can be specified multiple times)")
    private List<String> targetPackages;

    @Override
    public Integer call() throws IOException {
        var analyzer = JdepsAnalyzer.builder().jdepsFilter(buildJdepsFilter()).build();

        var graph = analyzer.analyze(sources, classpath);

        // Step 2: Extract reachable classes from dependency graph
        Map<String, Set<String>> reachableClasses = extractReachableClasses(graph);

        // Step 3: Execute shrink
        var shrinker = JarShrinker.builder().outputDir(outputDir).build();

        var result = shrinker.shrink(sources, reachableClasses);

        // Print results
        printShrinkResult(result);

        return 0;
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
