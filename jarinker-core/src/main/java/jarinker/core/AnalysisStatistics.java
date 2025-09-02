package jarinker.core;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics collected during analysis.
 *
 * @author Freeman
 */
public class AnalysisStatistics {

    private final int totalClasses;
    private final int usedClasses;
    private final int unusedClasses;
    private final Duration analysisTime;
    private final Map<String, Integer> packageClassCounts;

    public AnalysisStatistics(
            int totalClasses,
            int usedClasses,
            int unusedClasses,
            Duration analysisTime,
            Map<String, Integer> packageClassCounts) {
        this.totalClasses = totalClasses;
        this.usedClasses = usedClasses;
        this.unusedClasses = unusedClasses;
        this.analysisTime = analysisTime;
        this.packageClassCounts = new HashMap<>(packageClassCounts);
    }

    // Getter methods
    public int getTotalClasses() {
        return totalClasses;
    }

    public int getUsedClasses() {
        return usedClasses;
    }

    public int getUnusedClasses() {
        return unusedClasses;
    }

    public Duration getAnalysisTime() {
        return analysisTime;
    }

    public Map<String, Integer> getPackageClassCounts() {
        return new HashMap<>(packageClassCounts);
    }

    public double getUsageRatio() {
        return totalClasses == 0 ? 0.0 : (double) usedClasses / totalClasses;
    }

    @Override
    public String toString() {
        return "AnalysisStatistics{" + "totalClasses="
                + totalClasses + ", usedClasses="
                + usedClasses + ", unusedClasses="
                + unusedClasses + ", analysisTime="
                + analysisTime + ", packageClassCounts="
                + packageClassCounts + '}';
    }
}
