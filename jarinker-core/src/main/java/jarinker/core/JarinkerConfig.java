package jarinker.core;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration class for Jarinker operations.
 *
 * @author Freeman
 */
public class JarinkerConfig {

    // Configuration properties
    private final ShrinkStrategy strategy;
    private final Set<String> includePatterns;
    private final Set<String> excludePatterns;
    private final Optional<Path> outputDirectory; // Empty means in-place operation
    private final boolean inPlaceOperation; // Whether to enable in-place operation
    private final boolean verbose;
    private final boolean showProgress;
    private final Duration timeout;

    private JarinkerConfig(Builder builder) {
        this.strategy = builder.strategy;
        this.includePatterns = new HashSet<>(builder.includePatterns);
        this.excludePatterns = new HashSet<>(builder.excludePatterns);
        this.outputDirectory = builder.outputDirectory;
        this.inPlaceOperation = builder.inPlaceOperation;
        this.verbose = builder.verbose;
        this.showProgress = builder.showProgress;
        this.timeout = builder.timeout;
    }

    /**
     * Create a new builder instance.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getter methods
    public ShrinkStrategy getStrategy() {
        return strategy;
    }

    public Set<String> getIncludePatterns() {
        return new HashSet<>(includePatterns);
    }

    public Set<String> getExcludePatterns() {
        return new HashSet<>(excludePatterns);
    }

    public Optional<Path> getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isInPlaceOperation() {
        return inPlaceOperation;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Builder class for JarinkerConfig.
     */
    public static class Builder {
        private ShrinkStrategy strategy = ShrinkStrategy.DEFAULT;
        private Set<String> includePatterns = new HashSet<>();
        private Set<String> excludePatterns = new HashSet<>();
        private Optional<Path> outputDirectory = Optional.empty();
        private boolean inPlaceOperation = true;
        private boolean verbose = false;
        private boolean showProgress = true;
        private Duration timeout = Duration.ofMinutes(1);

        public Builder strategy(ShrinkStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder includePattern(String pattern) {
            this.includePatterns.add(pattern);
            return this;
        }

        public Builder includePatterns(Collection<String> patterns) {
            this.includePatterns.addAll(patterns);
            return this;
        }

        public Builder excludePattern(String pattern) {
            this.excludePatterns.add(pattern);
            return this;
        }

        public Builder excludePatterns(Collection<String> patterns) {
            this.excludePatterns.addAll(patterns);
            return this;
        }

        public Builder outputDirectory(Path path) {
            this.outputDirectory = Optional.ofNullable(path);
            return this;
        }

        public Builder enableInPlaceOperation() {
            this.inPlaceOperation = true;
            this.outputDirectory = Optional.empty();
            return this;
        }

        public Builder disableInPlaceOperation() {
            this.inPlaceOperation = false;
            return this;
        }

        public Builder inPlaceOperation(boolean inPlace) {
            this.inPlaceOperation = inPlace;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder showProgress(boolean show) {
            this.showProgress = show;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public JarinkerConfig build() {
            return new JarinkerConfig(this);
        }
    }
}
