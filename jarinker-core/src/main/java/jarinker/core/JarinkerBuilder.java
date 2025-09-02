package jarinker.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder for creating Jarinker instances with fluent API.
 *
 * @author Freeman
 */
public class JarinkerBuilder {

    private final List<Path> sources = new ArrayList<>();
    private final List<Path> dependencies = new ArrayList<>();
    private JarinkerConfig configuration;

    private JarinkerBuilder() {
        // Private constructor to enforce factory method usage
        this.configuration = JarinkerConfig.builder().build(); // Initialize with default config
    }

    /**
     * Create a new JarinkerBuilder instance.
     *
     * @return new JarinkerBuilder instance
     */
    public static JarinkerBuilder create() {
        return new JarinkerBuilder();
    }

    /**
     * Add source paths.
     *
     * @param paths source paths
     * @return this builder
     */
    public JarinkerBuilder withSource(Path... paths) {
        if (paths != null) {
            for (Path path : paths) {
                if (path != null) {
                    this.sources.add(path);
                }
            }
        }
        return this;
    }

    /**
     * Add source paths from collection.
     *
     * @param paths source paths collection
     * @return this builder
     */
    public JarinkerBuilder withSource(Collection<Path> paths) {
        if (paths != null) {
            for (Path path : paths) {
                if (path != null) {
                    this.sources.add(path);
                }
            }
        }
        return this;
    }

    /**
     * Add dependency paths.
     *
     * @param paths dependency paths
     * @return this builder
     */
    public JarinkerBuilder withDependencies(Path... paths) {
        if (paths != null) {
            for (Path path : paths) {
                if (path != null) {
                    this.dependencies.add(path);
                }
            }
        }
        return this;
    }

    /**
     * Add dependency paths from collection.
     *
     * @param paths dependency paths collection
     * @return this builder
     */
    public JarinkerBuilder withDependencies(Collection<Path> paths) {
        if (paths != null) {
            for (Path path : paths) {
                if (path != null) {
                    this.dependencies.add(path);
                }
            }
        }
        return this;
    }

    /**
     * Set output directory (optional, default: in-place operation).
     *
     * @param outputDir output directory
     * @return this builder
     */
    public JarinkerBuilder withOutputDirectory(Path outputDir) {
        this.configuration = JarinkerConfig.builder()
                .strategy(this.configuration.getStrategy())
                .includePatterns(this.configuration.getIncludePatterns())
                .excludePatterns(this.configuration.getExcludePatterns())
                .outputDirectory(outputDir)
                .inPlaceOperation(false)
                .verbose(this.configuration.isVerbose())
                .showProgress(this.configuration.isShowProgress())
                .timeout(this.configuration.getTimeout())
                .build();
        return this;
    }

    /**
     * Enable in-place operation (default behavior).
     *
     * @return this builder
     */
    public JarinkerBuilder enableInPlaceOperation() {
        this.configuration = JarinkerConfig.builder()
                .strategy(this.configuration.getStrategy())
                .includePatterns(this.configuration.getIncludePatterns())
                .excludePatterns(this.configuration.getExcludePatterns())
                .enableInPlaceOperation()
                .verbose(this.configuration.isVerbose())
                .showProgress(this.configuration.isShowProgress())
                .timeout(this.configuration.getTimeout())
                .build();
        return this;
    }

    /**
     * Disable in-place operation, must specify output directory.
     *
     * @return this builder
     */
    public JarinkerBuilder disableInPlaceOperation() {
        this.configuration = JarinkerConfig.builder()
                .strategy(this.configuration.getStrategy())
                .includePatterns(this.configuration.getIncludePatterns())
                .excludePatterns(this.configuration.getExcludePatterns())
                .disableInPlaceOperation()
                .verbose(this.configuration.isVerbose())
                .showProgress(this.configuration.isShowProgress())
                .timeout(this.configuration.getTimeout())
                .build();
        return this;
    }

    /**
     * Configure shrink strategy (currently only default strategy).
     *
     * @param strategy shrink strategy
     * @return this builder
     */
    public JarinkerBuilder withStrategy(ShrinkStrategy strategy) {
        this.configuration = JarinkerConfig.builder()
                .strategy(strategy)
                .includePatterns(this.configuration.getIncludePatterns())
                .excludePatterns(this.configuration.getExcludePatterns())
                .inPlaceOperation(this.configuration.isInPlaceOperation())
                .verbose(this.configuration.isVerbose())
                .showProgress(this.configuration.isShowProgress())
                .timeout(this.configuration.getTimeout())
                .build();
        return this;
    }

    /**
     * Set detailed configuration.
     *
     * @param config configuration
     * @return this builder
     */
    public JarinkerBuilder withConfiguration(JarinkerConfig config) {
        this.configuration = config;
        return this;
    }

    /**
     * Add include pattern.
     *
     * @param pattern include pattern
     * @return this builder
     */
    public JarinkerBuilder includePattern(String pattern) {
        this.configuration = JarinkerConfig.builder()
                .strategy(this.configuration.getStrategy())
                .includePatterns(this.configuration.getIncludePatterns())
                .includePattern(pattern)
                .excludePatterns(this.configuration.getExcludePatterns())
                .inPlaceOperation(this.configuration.isInPlaceOperation())
                .verbose(this.configuration.isVerbose())
                .showProgress(this.configuration.isShowProgress())
                .timeout(this.configuration.getTimeout())
                .build();
        return this;
    }

    /**
     * Add include patterns.
     *
     * @param patterns include patterns
     * @return this builder
     */
    public JarinkerBuilder includePatterns(Collection<String> patterns) {
        if (patterns != null) {
            for (String pattern : patterns) {
                includePattern(pattern);
            }
        }
        return this;
    }

    /**
     * Add exclude pattern.
     *
     * @param pattern exclude pattern
     * @return this builder
     */
    public JarinkerBuilder excludePattern(String pattern) {
        this.configuration = JarinkerConfig.builder()
                .strategy(this.configuration.getStrategy())
                .includePatterns(this.configuration.getIncludePatterns())
                .excludePatterns(this.configuration.getExcludePatterns())
                .excludePattern(pattern)
                .inPlaceOperation(this.configuration.isInPlaceOperation())
                .verbose(this.configuration.isVerbose())
                .showProgress(this.configuration.isShowProgress())
                .timeout(this.configuration.getTimeout())
                .build();
        return this;
    }

    /**
     * Add exclude patterns.
     *
     * @param patterns exclude patterns
     * @return this builder
     */
    public JarinkerBuilder excludePatterns(Collection<String> patterns) {
        if (patterns != null) {
            for (String pattern : patterns) {
                excludePattern(pattern);
            }
        }
        return this;
    }

    /**
     * Build Jarinker instance.
     *
     * @return Jarinker instance
     * @throws RuntimeException if configuration is invalid
     */
    public Jarinker build() {
        // Validate configuration
        if (sources.isEmpty()) {
            throw new RuntimeException("Source paths cannot be empty");
        }
        if (dependencies.isEmpty()) {
            throw new RuntimeException("Dependency paths cannot be empty");
        }

        return new Jarinker(new ArrayList<>(sources), new ArrayList<>(dependencies), configuration);
    }
}
