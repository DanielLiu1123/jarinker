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

    @Option(
            names = {"--include-jdk"},
            description = "Include JDK classes in analysis (default: false)")
    private boolean includeJdk;

    @Override
    public Integer call() throws IOException {
        // Build analyzer
        var analyzer = JdepsAnalyzer.builder().includeJdk(includeJdk).build();

        // Perform analysis
        DependencyGraph graph = analyzer.analyze(sources, classpath);

        // Print results
        printReport(graph);

        return 0;
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
