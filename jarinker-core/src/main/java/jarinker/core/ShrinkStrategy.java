package jarinker.core;

import java.util.Set;

/**
 * Strategy interface for determining which classes to keep during shrinking.
 *
 * @author Freeman
 */
public interface ShrinkStrategy {

    /**
     * Determine which classes are required and should be kept.
     *
     * @param analysis analysis result
     * @return set of required class names
     */
    Set<String> determineRequiredClasses(AnalysisResult analysis);

    /**
     * Get strategy name.
     *
     * @return strategy name
     */
    String getName();

    /**
     * Get strategy description.
     *
     * @return strategy description
     */
    String getDescription();

    /**
     * Default strategy instance.
     */
    ShrinkStrategy DEFAULT = new DefaultStrategy();
}
