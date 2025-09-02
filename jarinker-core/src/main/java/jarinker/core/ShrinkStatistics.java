package jarinker.core;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics collected during shrinking operation.
 *
 * @author Freeman
 */
public class ShrinkStatistics {

    private final long totalSizeBefore;
    private final long totalSizeAfter;
    private final int totalClassesBefore;
    private final int totalClassesAfter;
    private final Duration processingTime;
    private final Map<String, Long> jarSizeReductions;

    public ShrinkStatistics(
            long totalSizeBefore,
            long totalSizeAfter,
            int totalClassesBefore,
            int totalClassesAfter,
            Duration processingTime,
            Map<String, Long> jarSizeReductions) {
        this.totalSizeBefore = totalSizeBefore;
        this.totalSizeAfter = totalSizeAfter;
        this.totalClassesBefore = totalClassesBefore;
        this.totalClassesAfter = totalClassesAfter;
        this.processingTime = processingTime;
        this.jarSizeReductions = new HashMap<>(jarSizeReductions);
    }

    // Getter methods
    public long getTotalSizeBefore() {
        return totalSizeBefore;
    }

    public long getTotalSizeAfter() {
        return totalSizeAfter;
    }

    public int getTotalClassesBefore() {
        return totalClassesBefore;
    }

    public int getTotalClassesAfter() {
        return totalClassesAfter;
    }

    public Duration getProcessingTime() {
        return processingTime;
    }

    public Map<String, Long> getJarSizeReductions() {
        return new HashMap<>(jarSizeReductions);
    }

    public long getSizeSaved() {
        return totalSizeBefore - totalSizeAfter;
    }

    public double getShrinkRatio() {
        return totalSizeBefore == 0 ? 0.0 : (double) getSizeSaved() / totalSizeBefore;
    }

    @Override
    public String toString() {
        return "ShrinkStatistics{" + "totalSizeBefore="
                + totalSizeBefore + ", totalSizeAfter="
                + totalSizeAfter + ", totalClassesBefore="
                + totalClassesBefore + ", totalClassesAfter="
                + totalClassesAfter + ", processingTime="
                + processingTime + ", sizeSaved="
                + getSizeSaved() + ", shrinkRatio="
                + String.format("%.2f%%", getShrinkRatio() * 100) + '}';
    }
}
