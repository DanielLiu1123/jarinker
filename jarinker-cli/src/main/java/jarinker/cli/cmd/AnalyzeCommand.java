package jarinker.cli.cmd;

import com.sun.tools.jdeps.JdepsFilter;
import jarinker.core.DependencyGraph;
import jarinker.core.JdepsAnalyzer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
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
            description = "Find dependencies matching the given module name (can be specified multiple times)")
    private List<String> requires;

    @Option(
            names = {"--target-packages"},
            description = "Find dependencies matching the given package name (can be specified multiple times)")
    private List<String> targetPackages;

    @Override
    public Integer call() throws IOException {

        var analyzer = JdepsAnalyzer.builder().jdepsFilter(buildJdepsFilter()).build();

        // Perform analysis
        var graph = analyzer.analyze(sources, classpath);

        // Print results
        printReport(graph);

        return 0;
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
