package jarinker.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Default aggressive shrink strategy that removes all unused classes.
 *
 * @author Freeman
 */
public class DefaultStrategy implements ShrinkStrategy {

    @Override
    public Set<String> determineRequiredClasses(AnalysisResult analysis) {
        // Only keep directly dependent classes, maximum shrinking
        Set<String> requiredClasses = new HashSet<>();

        // Start from entry points and recursively collect all used classes
        for (String entryPoint : analysis.getEntryPoints()) {
            collectRequiredClasses(entryPoint, analysis, requiredClasses, new HashSet<>());
        }

        return requiredClasses;
    }

    private void collectRequiredClasses(
            String className, AnalysisResult analysis, Set<String> required, Set<String> visited) {
        if (!visited.add(className)) {
            return; // Already visited
        }

        required.add(className);

        // Recursively collect dependent classes
        Set<String> dependencies = analysis.getDependencyGraph().get(className);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                collectRequiredClasses(dependency, analysis, required, visited);
            }
        }
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public String getDescription() {
        return "Default aggressive shrink strategy that removes all unused classes";
    }
}
