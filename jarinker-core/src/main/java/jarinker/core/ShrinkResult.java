package jarinker.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Result of shrinking operation.
 *
 * @author Freeman
 */
public class ShrinkResult {

    // Shrunk JAR file information
    private final List<ShrunkJar> shrunkJars;

    // Shrink statistics
    private final ShrinkStatistics statistics;

    // Shrink warnings
    private final List<ShrinkWarning> warnings;

    // Shrink report
    private final Optional<ShrinkReport> report;

    public ShrinkResult(
            List<ShrunkJar> shrunkJars,
            ShrinkStatistics statistics,
            List<ShrinkWarning> warnings,
            Optional<ShrinkReport> report) {
        this.shrunkJars = new ArrayList<>(shrunkJars);
        this.statistics = statistics;
        this.warnings = new ArrayList<>(warnings);
        this.report = report;
    }

    // Getter methods
    public List<ShrunkJar> getShrunkJars() {
        return new ArrayList<>(shrunkJars);
    }

    public ShrinkStatistics getStatistics() {
        return statistics;
    }

    public List<ShrinkWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public Optional<ShrinkReport> getReport() {
        return report;
    }

    // Query methods
    public long getTotalSizeBefore() {
        return statistics.getTotalSizeBefore();
    }

    public long getTotalSizeAfter() {
        return statistics.getTotalSizeAfter();
    }

    public long getSizeSaved() {
        return statistics.getSizeSaved();
    }

    public double getShrinkRatio() {
        return statistics.getShrinkRatio();
    }

    public Duration getProcessingTime() {
        return statistics.getProcessingTime();
    }

    // Result validation
    public boolean isSuccessful() {
        return warnings.stream().noneMatch(w -> w.getType() == WarningType.GENERAL);
    }

    public List<String> getErrors() {
        List<String> errors = new ArrayList<>();
        for (ShrinkWarning warning : warnings) {
            if (warning.getType() == WarningType.GENERAL) {
                errors.add(warning.getMessage());
            }
        }
        return errors;
    }

    @Override
    public String toString() {
        return "ShrinkResult{" + "shrunkJars="
                + shrunkJars.size() + ", totalSizeBefore="
                + getTotalSizeBefore() + ", totalSizeAfter="
                + getTotalSizeAfter() + ", sizeSaved="
                + getSizeSaved() + ", shrinkRatio="
                + String.format("%.2f%%", getShrinkRatio() * 100) + ", processingTime="
                + getProcessingTime() + ", warnings="
                + warnings.size() + '}';
    }
}
