package jarinker.core;

import com.sun.tools.jdeps.Analyzer;

/**
 * @see com.sun.tools.jdeps.Analyzer.Type
 * @since 2025/9/6
 */
public enum AnalyzerType {
    MODULE,
    PACKAGE,
    CLASS;

    /**
     * Convert to jdeps analysis type.
     *
     * @return jdeps analysis type
     */
    public Analyzer.Type toJdepsAnalysisType() {
        return switch (this) {
            case MODULE -> Analyzer.Type.MODULE;
            case PACKAGE -> Analyzer.Type.PACKAGE;
            case CLASS -> Analyzer.Type.CLASS;
        };
    }
}
