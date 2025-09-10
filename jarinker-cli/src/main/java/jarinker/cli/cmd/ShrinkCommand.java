package jarinker.cli.cmd;

import com.sun.tools.jdeps.Archive;
import com.sun.tools.jdeps.JdepsFilter;
import jarinker.core.AnalyzerType;
import jarinker.core.DependencyGraph;
import jarinker.core.JarShrinker;
import jarinker.core.JdepsAnalyzer;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
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

    // === shrink options end ===

    @Option(
            names = {"--jar"},
            defaultValue = ".*",
            split = ",",
            description =
                    "Shrink JAR files matching the given pattern, shrink all jars by default. Supports comma-separated multiple patterns.")
    private List<Pattern> jarPatterns;

    @Override
    @SneakyThrows
    public void run() {

        DependencyGraph graph;

        try (var jdepsConfiguration = JdepsAnalyzer.buildJdepsConfiguration(sources, classpath, Runtime.version())) {
            var analyzer = JdepsAnalyzer.builder()
                    .jdepsFilter(buildJdepsFilter())
                    .jdepsConfiguration(jdepsConfiguration)
                    .type(AnalyzerType.CLASS)
                    .build();

            graph = analyzer.analyze();
        }

        var shrinker = JarShrinker.builder()
                .outputDir(outputDir)
                .jarPatterns(jarPatterns)
                .build();

        var result = shrinker.shrink(getDepJars(graph), graph);

        printShrinkResult(result);
    }

    @SneakyThrows
    private static List<Archive> getDepJars(DependencyGraph graph) {
        var classpath = new HashSet<>(graph.getArchives());
        for (var archive : graph.getRootArchives()) {
            classpath.remove(archive);
        }
        return List.copyOf(classpath);
    }

    private void printShrinkResult(JarShrinker.ShrinkResult result) {
        if (result.jars().isEmpty()) {
            System.out.println("🔍 No JAR files were processed.");
            return;
        }

        printHeader("JAR Shrinking Results");

        // Print individual JAR results
        for (var jar : result.jars()) {
            printJarResult(jar);
        }

        // Print summary statistics
        printSummaryStats(result);
    }

    private void printHeader(String title) {
        System.out.println("╭─" + "─".repeat(title.length()) + "─╮");
        System.out.println("│ " + title + " │");
        System.out.println("╰─" + "─".repeat(title.length()) + "─╯");
        System.out.println();
    }

    private void printJarResult(JarShrinker.ShrinkResult.Item jar) {
        String fileName = jar.before().toFile().getName();
        String beforeSize = formatBytes(jar.beforeSize());
        String afterSize = formatBytes(jar.afterSize());
        String savedSize = formatBytes(jar.getSavedBytes());
        double reductionPercentage = jar.getReductionPercentage();

        System.out.println("📦 " + fileName);
        System.out.println("   • Original size: " + beforeSize);
        System.out.println("   • Shrunk size:   " + afterSize);
        System.out.printf("   • Saved:         %s (%.2f%%)%n", savedSize, reductionPercentage);

        if (!jar.before().equals(jar.after())) {
            System.out.println("   • Output:        " + jar.after());
        }
        System.out.println();
    }

    private void printSummaryStats(JarShrinker.ShrinkResult result) {
        long totalOriginalSize = result.jars().stream()
                .mapToLong(JarShrinker.ShrinkResult.Item::beforeSize)
                .sum();

        long totalShrunkSize = result.jars().stream()
                .mapToLong(JarShrinker.ShrinkResult.Item::afterSize)
                .sum();

        long totalSaved = totalOriginalSize - totalShrunkSize;
        double totalReductionPercentage =
                totalOriginalSize > 0 ? ((double) totalSaved / totalOriginalSize) * 100.0 : 0.0;

        System.out.println("📊 Summary:");
        System.out.println("   • Processed JARs: " + result.jars().size());
        System.out.println("   • Total original size: " + formatBytes(totalOriginalSize));
        System.out.println("   • Total shrunk size:   " + formatBytes(totalShrunkSize));
        System.out.printf("   • Total saved:         %s (%.2f%%)%n", formatBytes(totalSaved), totalReductionPercentage);
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

        filterBuilder.filter(false, false);
        filterBuilder.findJDKInternals(false);
        filterBuilder.findMissingDeps(false);

        return filterBuilder.build();
    }
}
