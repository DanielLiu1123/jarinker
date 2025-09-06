package jarinker.cli.cmd;

import com.sun.tools.jdeps.JdepsFilter;
import jarinker.core.AnalyzerType;
import jarinker.core.DependencyGraph;
import jarinker.core.JdepsAnalyzer;
import java.nio.file.Path;
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
 * Analyze command for dependency analysis.
 *
 * @author Freeman
 */
@Command(description = "Analyze dependencies and generate dependency graph", mixinStandardHelpOptions = true)
public class AnalyzeCommand implements Runnable {

    @Parameters(description = "Source artifacts to analyze (JAR files or class directories)", arity = "1..*")
    private List<Path> sources;

    @Option(
            names = {"-cp", "-classpath", "--class-path"},
            description = "Classpath entries (can be specified multiple times)",
            required = true)
    private List<Path> classpath;

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
            description = "Find dependencies matching the given module name (can be specified multiple times)")
    private @Nullable List<String> requires;

    @Option(
            names = {"--target-packages"},
            description = "Find dependencies matching the given package name (can be specified multiple times)")
    private @Nullable List<String> targetPackages;

    @Option(
            names = {"--type"},
            description = "Analysis type (class, package, module), see jarinker.core.AnalyzerType",
            defaultValue = "class")
    private AnalyzerType type;

    // === jdeps options end ===

    @Override
    @SneakyThrows
    public void run() {

        DependencyGraph graph;

        try (var jdepsConfiguration = JdepsAnalyzer.buildJdepsConfiguration(sources, classpath, Runtime.version())) {
            var analyzer = JdepsAnalyzer.builder()
                    .jdepsFilter(buildJdepsFilter())
                    .jdepsConfiguration(jdepsConfiguration)
                    .type(type)
                    .build();

            graph = analyzer.analyze();
        }

        // Print results
        printReport(graph);
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

    private void printReport(DependencyGraph graph) {
        switch (graph.getAnalysisType()) {
            case CLASS -> printReportForClass(graph);
            case PACKAGE -> printReportForPackage(graph);
            case MODULE -> printReportForModule(graph);
        }
    }

    private void printReportForModule(DependencyGraph graph) {
        printHeader("Module Dependency Analysis");
        printSummaryStats(graph);
        System.out.println();
        printDependenciesByType(graph, "Module");
    }

    private void printReportForClass(DependencyGraph graph) {
        printHeader("Class Dependency Analysis");
        printSummaryStats(graph);
        System.out.println();
        printDependenciesByType(graph, "Class");
    }

    private void printReportForPackage(DependencyGraph graph) {
        printHeader("Package Dependency Analysis");
        printSummaryStats(graph);
        System.out.println();
        printDependenciesByType(graph, "Package");
    }

    private void printHeader(String title) {
        System.out.println("╭─" + "─".repeat(title.length()) + "─╮");
        System.out.println("│ " + title + " │");
        System.out.println("╰─" + "─".repeat(title.length()) + "─╯");
        System.out.println();
    }

    private void printSummaryStats(DependencyGraph graph) {
        var dependenciesMap = graph.getDependenciesMap();

        // Calculate statistics excluding JDK dependencies
        int nodesWithNonJdkDeps = 0;
        int totalNonJdkDependencies = 0;

        for (var entry : dependenciesMap.entrySet()) {
            Set<String> nonJdkDeps = entry.getValue().stream()
                    .filter(dep -> !isJdkDependency(dep))
                    .collect(java.util.stream.Collectors.toSet());

            if (!nonJdkDeps.isEmpty()) {
                nodesWithNonJdkDeps++;
                totalNonJdkDependencies += nonJdkDeps.size();
            }
        }

        System.out.println("📊 Statistics (excluding JDK dependencies):");
        System.out.println("   • Total nodes: " + graph.getNodeCount());
        System.out.println("   • Nodes with non-JDK dependencies: " + nodesWithNonJdkDeps);
        System.out.println("   • Total non-JDK dependencies: " + totalNonJdkDependencies);
        System.out.println("   • Root nodes: " + graph.getRootNodes().size());
        System.out.println("   • Leaf nodes: " + graph.getLeafNodes().size());
    }

    private void printDependenciesByType(DependencyGraph graph, String nodeType) {
        var dependenciesMap = graph.getDependenciesMap();

        if (dependenciesMap.isEmpty()) {
            System.out.println("🔍 No dependencies found.");
            return;
        }

        System.out.println("🔗 " + nodeType + " Dependencies:");
        System.out.println();

        dependenciesMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String source = entry.getKey();
            Set<String> dependencies = entry.getValue();

            // Filter out JDK dependencies
            Set<String> filteredDependencies = dependencies.stream()
                    .filter(dep -> !isJdkDependency(dep))
                    .collect(java.util.stream.Collectors.toSet());

            if (!filteredDependencies.isEmpty()) {
                System.out.println("📦 " + source);
                filteredDependencies.stream().sorted().forEach(dep -> System.out.println("   └─ " + dep));
                System.out.println();
            }
        });
    }

    /**
     * Check if a dependency is a JDK internal dependency that should be filtered out.
     *
     * @param dependency the dependency name
     * @return true if it's a JDK dependency
     */
    private static boolean isJdkDependency(String dependency) {
        return dependency.startsWith("java.")
                || dependency.startsWith("javax.")
                || dependency.startsWith("jdk.")
                || dependency.startsWith("sun.")
                || dependency.startsWith("com.sun.")
                || dependency.contains("JDK removed internal API");
    }
}
