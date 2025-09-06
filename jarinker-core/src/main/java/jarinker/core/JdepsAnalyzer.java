package jarinker.core;

import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.JdepsConfiguration;
import com.sun.tools.jdeps.JdepsFilter;
import com.sun.tools.jdeps.JdepsWriter;
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
    private JdepsConfiguration jdepsConfiguration;
    private AnalyzerType type;

    /**
     * Analyze dependencies using jdeps.
     *
     * @return dependency graph
     */
    public DependencyGraph analyze() {
        return doAnalysis();
    }

    @SneakyThrows
    private DependencyGraph doAnalysis() {
        var depsAnalyzer = new DepsAnalyzer(
                jdepsConfiguration, jdepsFilter, buildJdepsWriter(), type.toJdepsAnalysisType(), false);

        var ok = depsAnalyzer.run(false, Integer.MAX_VALUE);
        if (!ok) {
            throw new RuntimeException("Jdeps analysis failed");
        }

        return new DependencyGraph(depsAnalyzer.dependenceGraph(), type);
    }

    private JdepsWriter buildJdepsWriter() {
        return JdepsWriter.newSimpleWriter(new PrintWriter(new StringWriter()), type.toJdepsAnalysisType());
    }

    @SneakyThrows
    public static JdepsConfiguration buildJdepsConfiguration(
            List<Path> sources, List<Path> classpath, Runtime.Version multiReleaseVersion) {
        var builder = new JdepsConfiguration.Builder();

        builder.multiRelease(multiReleaseVersion);

        // Add sources
        for (Path source : sources) {
            if (!source.toFile().exists()) {
                throw new IllegalArgumentException("Source path does not exist: " + source);
            }
            builder.addRoot(source);
        }

        // Add classpath
        for (Path cp : classpath) {
            cp = cp.toAbsolutePath().toString().endsWith("/*") ? cp.getParent() : cp;
            if (cp == null || !Files.exists(cp)) {
                continue;
            }
            if (Files.isDirectory(cp)) {
                try (var stream = Files.walk(cp)) {
                    stream.filter(Files::isRegularFile)
                            .map(e -> e.toAbsolutePath().toString())
                            .filter(p -> p.endsWith(".jar"))
                            .forEach(builder::addClassPath);
                }
            } else {
                builder.addClassPath(cp.toAbsolutePath().toString());
            }
        }

        return builder.build();
    }
}
