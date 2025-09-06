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
        // Try with multi-release support first
        try {
            return analyzeWithMultiRelease(sources, classpath);
        } catch (Exception e) {
            // If multi-release analysis fails, try with Java 8 compatibility mode
            if (isMultiReleaseError(e)) {
                System.err.println("Warning: Multi-release analysis failed, falling back to Java 8 compatibility mode");
                return analyzeWithJava8Compatibility(sources, classpath);
            }
            // Re-throw other exceptions
            if (e instanceof IOException) {
                throw e;
            }
            throw new IOException("Failed to analyze dependencies: " + e.getMessage(), e);
        }
    }

    private boolean isMultiReleaseError(Exception e) {
        // Check for module system related errors
        if (e.getCause() instanceof java.lang.module.FindException) {
            return true;
        }

        // Check for multi-release JAR issues
        String message = e.getMessage();
        if (message != null) {
            return message.contains("Module") && message.contains("not found")
                    || message.contains("multi-release jar file but --multi-release option is not set");
        }

        return false;
    }

    private DependencyGraph analyzeWithMultiRelease(List<Path> sources, List<Path> classpath) throws IOException {
        return performAnalysis(sources, classpath, Runtime.version());
    }

    private DependencyGraph analyzeWithJava8Compatibility(List<Path> sources, List<Path> classpath) throws IOException {
        return performAnalysis(sources, classpath, Runtime.Version.parse("8"));
    }

    private DependencyGraph performAnalysis(
            List<Path> sources, List<Path> classpath, Runtime.Version multiReleaseVersion) throws IOException {
        try {
            // Build jdeps configuration
            var configBuilder = new JdepsConfiguration.Builder();
            if (multiReleaseVersion != null) {
                configBuilder.multiRelease(multiReleaseVersion);
            }

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
