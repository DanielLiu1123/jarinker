package jarinker.core;

/**
 * Types of warnings that can be generated during analysis or shrinking.
 *
 * @author Freeman
 */
public enum WarningType {

    /**
     * Class not found in classpath.
     */
    CLASS_NOT_FOUND,

    /**
     * Potential reflection usage detected.
     */
    REFLECTION_USAGE,

    /**
     * Circular dependency detected.
     */
    CIRCULAR_DEPENDENCY,

    /**
     * Missing dependency.
     */
    MISSING_DEPENDENCY,

    /**
     * Unused class detected.
     */
    UNUSED_CLASS,

    /**
     * Large class that might impact performance.
     */
    LARGE_CLASS,

    /**
     * Annotation processing related warning.
     */
    ANNOTATION_PROCESSING,

    /**
     * General analysis warning.
     */
    GENERAL
}
