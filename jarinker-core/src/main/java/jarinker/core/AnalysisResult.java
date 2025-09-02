package jarinker.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Result of dependency analysis.
 *
 * @author Freeman
 */
public class AnalysisResult {

    // All discovered class information
    private final Map<String, ClassInfo> allClasses;

    // Dependency relationship graph
    private final Map<String, Set<String>> dependencyGraph;

    // Entry point classes
    private final Set<String> entryPoints;

    // Analysis warnings
    private final List<AnalysisWarning> warnings;

    // Analysis statistics
    private final AnalysisStatistics statistics;

    public AnalysisResult(
            Map<String, ClassInfo> allClasses,
            Map<String, Set<String>> dependencyGraph,
            Set<String> entryPoints,
            List<AnalysisWarning> warnings,
            AnalysisStatistics statistics) {
        this.allClasses = new HashMap<>(allClasses);
        this.dependencyGraph = new HashMap<>(dependencyGraph);
        this.entryPoints = new HashSet<>(entryPoints);
        this.warnings = new ArrayList<>(warnings);
        this.statistics = statistics;
    }

    // Getter methods
    public Map<String, ClassInfo> getAllClasses() {
        return new HashMap<>(allClasses);
    }

    public Map<String, Set<String>> getDependencyGraph() {
        return new HashMap<>(dependencyGraph);
    }

    public Set<String> getEntryPoints() {
        return new HashSet<>(entryPoints);
    }

    public List<AnalysisWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public AnalysisStatistics getStatistics() {
        return statistics;
    }

    // Query methods
    public Set<String> getUnusedClasses() {
        Set<String> usedClasses = new HashSet<>();
        for (Set<String> deps : dependencyGraph.values()) {
            usedClasses.addAll(deps);
        }
        usedClasses.addAll(entryPoints);

        Set<String> unusedClasses = new HashSet<>(allClasses.keySet());
        unusedClasses.removeAll(usedClasses);
        return unusedClasses;
    }

    public Set<String> getDependencies(String className) {
        return new HashSet<>(dependencyGraph.getOrDefault(className, new HashSet<>()));
    }

    public Set<String> getDependents(String className) {
        Set<String> dependents = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            if (entry.getValue().contains(className)) {
                dependents.add(entry.getKey());
            }
        }
        return dependents;
    }

    public ClassInfo getClassInfo(String className) {
        ClassInfo classInfo = allClasses.get(className);
        if (classInfo == null) {
            throw new IllegalArgumentException("Class not found: " + className);
        }
        return classInfo;
    }

    public boolean isClassUsed(String className) {
        return !getUnusedClasses().contains(className);
    }

    public List<String> getDependencyPath(String from, String to) {
        // Simple BFS to find dependency path
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();
        List<String> queue = new ArrayList<>();

        queue.add(from);
        visited.add(from);
        parent.put(from, null);

        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            if (current.equals(to)) {
                // Reconstruct path
                List<String> path = new ArrayList<>();
                String node = to;
                while (node != null) {
                    path.add(0, node);
                    node = parent.get(node);
                }
                return path;
            }

            Set<String> deps = dependencyGraph.getOrDefault(current, new HashSet<>());
            for (String dep : deps) {
                if (!visited.contains(dep)) {
                    visited.add(dep);
                    parent.put(dep, current);
                    queue.add(dep);
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    // Statistics methods
    public int getTotalClassCount() {
        return allClasses.size();
    }

    public int getUsedClassCount() {
        return getTotalClassCount() - getUnusedClassCount();
    }

    public int getUnusedClassCount() {
        return getUnusedClasses().size();
    }

    public double getUsageRatio() {
        return getTotalClassCount() == 0 ? 0.0 : (double) getUsedClassCount() / getTotalClassCount();
    }
}
