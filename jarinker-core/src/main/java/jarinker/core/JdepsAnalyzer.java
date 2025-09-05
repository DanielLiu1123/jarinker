package jarinker.core;

import com.sun.tools.jdeps.Analyzer;
import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.JdepsConfiguration;
import com.sun.tools.jdeps.JdepsFilter;
import com.sun.tools.jdeps.JdepsWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

/**
 * Wrapper around jdeps DepsAnalyzer for dependency analysis.
 * Directly uses jdeps internal API and data models.
 *
 * @author Freeman
 */
public class JdepsAnalyzer {

    private final boolean includeJdk;

    public JdepsAnalyzer(boolean includeJdk) {
        this.includeJdk = includeJdk;
    }

    /**
     * Analyze dependencies using jdeps.
     *
     * @param sources source paths to analyze
     * @param classpath classpath for analysis
     * @return dependency graph
     * @throws IOException if analysis fails
     */
    public DependencyGraph analyze(List<Path> sources, List<Path> classpath) throws IOException {

        try {
            // Build jdeps configuration
            var configBuilder =
                    new JdepsConfiguration.Builder().multiRelease(Runtime.version()); // Support multi-release JARs

            // Add sources
            for (Path source : sources) {
                if (!source.toFile().exists()) {
                    throw new IOException("Source path does not exist: " + source);
                }
                configBuilder.addRoot(source);
            }

            // Add classpath
            for (Path cp : classpath) {
                if (cp.toFile().exists()) {
                    configBuilder.addClassPath(cp.toString());
                }
            }

            var config = configBuilder.build();

            // Build filter - create a simple filter that optionally excludes JDK
            var filterBuilder = new JdepsFilter.Builder();
            if (!includeJdk) {
                // Filter out JDK internal packages
                filterBuilder.filter(true, true);
            }
            var filter = filterBuilder.build();

            // Create a null writer since we only need the graph
            var writer = JdepsWriter.newSimpleWriter(new PrintWriter(new StringWriter()), Analyzer.Type.CLASS);

            // Create and run analyzer
            var depsAnalyzer = new DepsAnalyzer(config, filter, writer, Analyzer.Type.CLASS, false);

            boolean success = depsAnalyzer.run();
            if (!success) {
                throw new IOException("jdeps analysis failed - no dependencies found or analysis error");
            }

            var graph = depsAnalyzer.dependenceGraph();
            if (graph == null || graph.nodes().isEmpty()) {
                throw new IOException("jdeps analysis produced empty dependency graph");
            }

            return new DependencyGraph(graph);

        } catch (Exception e) {
            if (e instanceof IOException) {
                throw e;
            }
            throw new IOException("Failed to analyze dependencies: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new builder for JdepsAnalyzer.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for JdepsAnalyzer.
     */
    public static class Builder {
        private boolean includeJdk = false;

        public Builder includeJdk(boolean includeJdk) {
            this.includeJdk = includeJdk;
            return this;
        }

        public JdepsAnalyzer build() {
            return new JdepsAnalyzer(includeJdk);
        }
    }
}
