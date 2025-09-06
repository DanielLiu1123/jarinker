package jarinker.cli.cmd;

import jarinker.core.DependencyGraph;
import jarinker.core.JdepsAnalyzer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Analyze command for dependency analysis.
 *
 * @author Freeman
 */
@Command(description = "Analyze dependencies and generate dependency graph", mixinStandardHelpOptions = true)
public class AnalyzeCommand implements Callable<Integer> {

    @Parameters(description = "Source artifacts to analyze (JAR files or class directories)", arity = "1..*")
    private List<Path> sources;

    @Option(
            names = {"-cp", "-classpath", "--class-path"},
            description = "Classpath entries (can be specified multiple times)",
            required = true)
    private List<Path> classpath;

    // Filter options
    @Option(
            names = {"--filter-pattern"},
            description = "Filter dependencies matching the given pattern")
    private String filterPattern;

    @Option(
            names = {"--regex"},
            description = "Find dependencies matching the given pattern")
    private String regex;

    @Option(
            names = {"--filter-same-package"},
            description = "Filter dependencies within the same package (default: true)")
    private boolean filterSamePackage = true;

    @Option(
            names = {"--filter-same-archive"},
            description = "Filter dependencies within the same archive (default: false)")
    private boolean filterSameArchive = false;

    @Option(
            names = {"--find-jdk-internals"},
            description = "Find class-level dependencies on JDK internal APIs")
    private boolean findJDKInternals = false;

    @Option(
            names = {"--find-missing-deps"},
            description = "Find missing dependencies")
    private boolean findMissingDeps = false;

    // Source filters
    @Option(
            names = {"--include-pattern"},
            description = "Restrict analysis to classes matching pattern")
    private String includePattern;

    @Option(
            names = {"--requires"},
            description = "Find dependencies matching the given module name (can be specified multiple times)")
    private List<String> requires;

    @Option(
            names = {"--target-packages"},
            description = "Find dependencies matching the given package name (can be specified multiple times)")
    private List<String> targetPackages;

    @Override
    public Integer call() throws IOException {
        // Build analyzer with filter
        var filter = buildJdepsFilter();
        var analyzerBuilder = JdepsAnalyzer.builder();

        if (filter != null) {
            analyzerBuilder.jdepsFilter(filter);
        }

        var analyzer = analyzerBuilder.build();

        // Perform analysis
        DependencyGraph graph = analyzer.analyze(sources, classpath);

        // Print results
        printReport(graph);

        return 0;
    }

    /**
     * Build JdepsFilterBuilder with all configured options.
     *
     * @return configured JdepsFilterBuilder
     */
    private JdepsFilterBuilder buildJdepsFilter() {
        var filterBuilder = new JdepsFilterBuilder();

        // Apply filter options
        if (filterPattern != null) {
            filterBuilder.filterPattern(filterPattern);
        }

        if (regex != null) {
            filterBuilder.regex(regex);
        }

        filterBuilder.filterSamePackage(filterSamePackage);
        filterBuilder.filterSameArchive(filterSameArchive);
        filterBuilder.findJDKInternals(findJDKInternals);
        filterBuilder.findMissingDeps(findMissingDeps);

        if (includePattern != null) {
            filterBuilder.includePattern(includePattern);
        }

        if (requires != null) {
            for (String require : requires) {
                filterBuilder.addRequire(require);
            }
        }

        if (targetPackages != null) {
            for (String pkg : targetPackages) {
                filterBuilder.addTargetPackage(pkg);
            }
        }

        return filterBuilder;
    }

    private void printReport(DependencyGraph graph) {
        System.out.println("=== Dependency Analysis Report ===");
        System.out.println("Total nodes: " + graph.getNodeCount());

        System.out.println("\n=== Dependencies ===");
        for (String nodeName : graph.getNodeNames()) {
            var dependencies = graph.getDependencies(nodeName);
            if (!dependencies.isEmpty()) {
                System.out.println(nodeName + " -> " + dependencies);
            }
        }
    }
}
