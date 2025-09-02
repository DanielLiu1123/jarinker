package jarinker.cli.cmd.shrink;

import static jarinker.cli.util.Printer.print;

import jarinker.core.JarinkerBuilder;
import jarinker.core.ShrinkResult;
import jarinker.core.ShrinkStrategy;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Shrink command implementation according to new API specification.
 *
 * @author Freeman
 */
@Command(
        name = "shrink",
        mixinStandardHelpOptions = true,
        description = "Shrink the JAR files by removing unused classes.")
public class ShrinkCommand implements Callable<Integer> {

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

    @Option(
            names = {"-o", "--output"},
            type = Path.class,
            description = "Output directory (optional, default: in-place operation)")
    private Path output;

    @Option(
            names = {"--in-place"},
            description = "Force in-place operation (default: true)")
    private boolean inPlace = true;

    @Option(
            names = {"--show-progress"},
            description = "Show progress information (default: true)")
    private boolean showProgress = true;

    @Option(
            names = {"--timeout"},
            description = "Processing timeout (default: 1m)")
    private String timeout = "1m";

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

            // Parse timeout
            Duration timeoutDuration = parseTimeout(timeout);

            // Build Jarinker configuration
            var configBuilder = jarinker.core.JarinkerConfig.builder()
                    .strategy(ShrinkStrategy.DEFAULT)
                    .verbose(verbose)
                    .showProgress(showProgress)
                    .timeout(timeoutDuration);

            // Add include patterns
            for (String pattern : includePatterns) {
                configBuilder.includePattern(pattern);
            }

            // Add exclude patterns
            for (String pattern : excludePatterns) {
                configBuilder.excludePattern(pattern);
            }

            // Configure output
            if (output != null) {
                configBuilder.outputDirectory(output).disableInPlaceOperation();
            } else if (inPlace) {
                configBuilder.enableInPlaceOperation();
            }

            var config = configBuilder.build();

            // Build Jarinker instance
            var jarinker = JarinkerBuilder.create()
                    .withSource(sources)
                    .withDependencies(dependencies)
                    .withConfiguration(config)
                    .build();

            // Execute shrinking
            print("Starting JAR shrinking...");
            ShrinkResult result = jarinker.shrink();

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

    private Duration parseTimeout(String timeoutStr) {
        try {
            if (timeoutStr.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)));
            } else if (timeoutStr.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)));
            } else if (timeoutStr.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)));
            } else {
                return Duration.ofMinutes(Long.parseLong(timeoutStr));
            }
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid timeout format '" + timeoutStr + "', using default 1m");
            return Duration.ofMinutes(1);
        }
    }

    private void displayResults(ShrinkResult result) {
        print("");
        print("=== Shrinking Results ===");
        print("Shrunk JARs: %d".formatted(result.getShrunkJars().size()));
        print("Total size before: %s".formatted(formatSize(result.getTotalSizeBefore())));
        print("Total size after: %s".formatted(formatSize(result.getTotalSizeAfter())));
        print("Size saved: %s".formatted(formatSize(result.getSizeSaved())));
        print("Shrink ratio: %.2f%%".formatted(result.getShrinkRatio() * 100));
        print("Processing time: %s".formatted(formatDuration(result.getProcessingTime())));

        if (!result.getWarnings().isEmpty()) {
            print("");
            print("=== Warnings ===");
            for (var warning : result.getWarnings()) {
                print("- %s: %s".formatted(warning.getType(), warning.getMessage()));
            }
        }

        if (verbose && !result.getShrunkJars().isEmpty()) {
            print("");
            print("=== Detailed Results ===");
            for (var jar : result.getShrunkJars()) {
                print("JAR: %s".formatted(jar.getOriginalName()));
                print("  Original size: %s (%d classes)"
                        .formatted(formatSize(jar.getOriginalSize()), jar.getOriginalClassCount()));
                print("  Shrunk size: %s (%d classes)"
                        .formatted(formatSize(jar.getShrunkSize()), jar.getShrunkClassCount()));
                print("  Saved: %s (%.2f%%)".formatted(formatSize(jar.getSizeSaved()), jar.getShrinkRatio() * 100));
                print("  Output: %s".formatted(jar.getOutputPath()));
            }
        }

        print("");
        if (result.isSuccessful()) {
            print("✓ Shrinking completed successfully!");
        } else {
            print("⚠ Shrinking completed with warnings");
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m " + (seconds % 60) + "s";
    }
}
