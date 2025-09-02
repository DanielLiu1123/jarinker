package jarinker.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed report of shrinking operation.
 *
 * @author Freeman
 */
public class ShrinkReport {

    private final LocalDateTime timestamp;
    private final String summary;
    private final List<String> removedClasses;
    private final List<String> keptClasses;
    private final ShrinkStatistics statistics;

    public ShrinkReport(
            LocalDateTime timestamp,
            String summary,
            List<String> removedClasses,
            List<String> keptClasses,
            ShrinkStatistics statistics) {
        this.timestamp = timestamp;
        this.summary = summary;
        this.removedClasses = new ArrayList<>(removedClasses);
        this.keptClasses = new ArrayList<>(keptClasses);
        this.statistics = statistics;
    }

    // Getter methods
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getRemovedClasses() {
        return new ArrayList<>(removedClasses);
    }

    public List<String> getKeptClasses() {
        return new ArrayList<>(keptClasses);
    }

    public ShrinkStatistics getStatistics() {
        return statistics;
    }

    @Override
    public String toString() {
        return "ShrinkReport{" + "timestamp="
                + timestamp + ", summary='"
                + summary + '\'' + ", removedClasses="
                + removedClasses.size() + ", keptClasses="
                + keptClasses.size() + ", statistics="
                + statistics + '}';
    }
}
