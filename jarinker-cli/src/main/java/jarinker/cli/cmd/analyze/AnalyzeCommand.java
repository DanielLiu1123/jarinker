package jarinker.cli.cmd.analyze;

import static jarinker.cli.util.Printer.print;

import jarinker.core.AnalysisResult;
import jarinker.core.JarinkerBuilder;
import jarinker.core.ShrinkStrategy;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Analyze command implementation according to new API specification.
 *
 * @author Freeman
 */
@Command(name = "analyze", mixinStandardHelpOptions = true, description = "Analyze JAR dependencies without shrinking.")
public class AnalyzeCommand implements Callable<Integer> {

    @Option(
            names = {"-s", "--sources"},
            required = true,
            type = Path.class,
            description = "Source paths (required, can be specified multiple times)")
    private List<Path> sources = new ArrayList<>();

    @Option(
            names = {"-d", "--dependencies"},
            required = true,
            type = Path.class,
            description = "Dependency paths (required, can be specified multiple times)")
    private List<Path> dependencies = new ArrayList<>();

    @Option(
            names = {"--include"},
            description = "Include pattern (can be specified multiple times)")
    private List<String> includePatterns = new ArrayList<>();

    @Option(
            names = {"--exclude"},
            description = "Exclude pattern (can be specified multiple times)")
    private List<String> excludePatterns = new ArrayList<>();

    @Option(
            names = {"--verbose"},
            description = "Enable verbose output")
    private boolean verbose = false;

    @Override
    public Integer call() throws Exception {
        try {
            // Validate inputs
            if (sources.isEmpty()) {
                System.err.println("Error: Source paths cannot be empty");
                return 1;
            }
            if (dependencies.isEmpty()) {
                System.err.println("Error: Dependency paths cannot be empty");
                return 1;
            }

            // Build Jarinker configuration
            var configBuilder = jarinker.core.JarinkerConfig.builder()
                    .strategy(ShrinkStrategy.DEFAULT)
                    .verbose(verbose)
                    .showProgress(true)
                    .timeout(Duration.ofMinutes(1));

            // Add include patterns
            for (String pattern : includePatterns) {
                configBuilder.includePattern(pattern);
            }

            // Add exclude patterns
            for (String pattern : excludePatterns) {
                configBuilder.excludePattern(pattern);
            }

            var config = configBuilder.build();

            // Build Jarinker instance
            var jarinker = JarinkerBuilder.create()
                    .withSource(sources)
                    .withDependencies(dependencies)
                    .withConfiguration(config)
                    .build();

            // Execute analysis
            print("Starting dependency analysis...");
            AnalysisResult result = jarinker.analyze();

            // Display results
            displayResults(result);

            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private void displayResults(AnalysisResult result) {
        print("");
        print("=== Analysis Results ===");
        print("Total classes: %d".formatted(result.getTotalClassCount()));
        print("Used classes: %d".formatted(result.getUsedClassCount()));
        print("Unused classes: %d".formatted(result.getUnusedClassCount()));
        print("Usage ratio: %.2f%%".formatted(result.getUsageRatio() * 100));
        print("Analysis time: %s"
                .formatted(formatDuration(result.getStatistics().getAnalysisTime())));

        if (!result.getWarnings().isEmpty()) {
            print("");
            print("=== Warnings ===");
            for (var warning : result.getWarnings()) {
                print("- %s: %s".formatted(warning.getType(), warning.getMessage()));
                if (warning.getSuggestion().isPresent()) {
                    print("  Suggestion: %s".formatted(warning.getSuggestion().get()));
                }
            }
        }

        if (verbose) {
            print("");
            print("=== Package Statistics ===");
            var packageCounts = result.getStatistics().getPackageClassCounts();
            packageCounts.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(10)
                    .forEach(entry -> print("  %s: %d classes"
                            .formatted(
                                    entry.getKey().isEmpty() ? "(default package)" : entry.getKey(),
                                    entry.getValue())));

            if (packageCounts.size() > 10) {
                print("  ... and %d more packages".formatted(packageCounts.size() - 10));
            }

            print("");
            print("=== Entry Points ===");
            var entryPoints = result.getEntryPoints();
            if (entryPoints.size() <= 20) {
                entryPoints.forEach(className -> print("  " + className));
            } else {
                entryPoints.stream().limit(20).forEach(className -> print("  " + className));
                print("  ... and %d more entry points".formatted(entryPoints.size() - 20));
            }

            print("");
            print("=== Unused Classes (Sample) ===");
            var unusedClasses = result.getUnusedClasses();
            if (unusedClasses.isEmpty()) {
                print("  No unused classes found");
            } else if (unusedClasses.size() <= 20) {
                unusedClasses.forEach(className -> print("  " + className));
            } else {
                unusedClasses.stream().limit(20).forEach(className -> print("  " + className));
                print("  ... and %d more unused classes".formatted(unusedClasses.size() - 20));
            }
        }

        print("");
        print("✓ Analysis completed successfully!");
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m " + (seconds % 60) + "s";
    }
}
