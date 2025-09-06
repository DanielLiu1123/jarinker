package jarinker.core;

import com.sun.tools.jdeps.Analyzer;
import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.JdepsConfiguration;
import com.sun.tools.jdeps.JdepsFilter;
import com.sun.tools.jdeps.JdepsWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.Builder;
import lombok.SneakyThrows;

/**
 * Wrapper around jdeps DepsAnalyzer for dependency analysis.
 * Directly uses jdeps internal API and data models.
 *
 * @author Freeman
 */
@Builder
public class JdepsAnalyzer {

    private JdepsFilter jdepsFilter;

    /**
     * Analyze dependencies using jdeps.
     *
     * @param sources   source paths to analyze
     * @param classpath classpath for analysis
     * @return dependency graph
     */
    @SneakyThrows
    public DependencyGraph analyze(List<Path> sources, List<Path> classpath) {
        return performAnalysis(sources, classpath, Runtime.version());
    }

    @SneakyThrows
    private DependencyGraph performAnalysis(
            List<Path> sources, List<Path> classpath, Runtime.Version multiReleaseVersion) {
        // Build jdeps configuration
        var configBuilder = new JdepsConfiguration.Builder();
        configBuilder.multiRelease(multiReleaseVersion);

        // Add sources
        for (Path source : sources) {
            if (!source.toFile().exists()) {
                throw new IOException("Source path does not exist: " + source);
            }
            configBuilder.addRoot(source);
        }

        // Add classpath
        for (Path cp : classpath) {
            cp = cp.toAbsolutePath().toString().endsWith("/*") ? cp.getParent() : cp;
            if (cp != null && Files.exists(cp) && Files.isDirectory(cp)) {
                try (var stream = Files.walk(cp)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().endsWith(".jar"))
                            .map(p -> p.toAbsolutePath().toString())
                            .forEach(configBuilder::addClassPath);
                }
            }
        }

        var config = configBuilder.build();

        // Create a null writer since we only need the graph
        var writer = JdepsWriter.newSimpleWriter(new PrintWriter(new StringWriter()), Analyzer.Type.CLASS);

        // Create and run analyzer
        var depsAnalyzer = new DepsAnalyzer(config, jdepsFilter, writer, Analyzer.Type.CLASS, false);

        boolean success = depsAnalyzer.run();
        if (!success) {
            throw new IOException("jdeps analysis failed - no dependencies found or analysis error");
        }

        var graph = depsAnalyzer.dependenceGraph();
        if (graph == null || graph.nodes().isEmpty()) {
            throw new IOException("jdeps analysis produced empty dependency graph");
        }

        return new DependencyGraph(graph);
    }
}
