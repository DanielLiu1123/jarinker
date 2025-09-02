package jarinker.core;

import java.util.Optional;

/**
 * Warning generated during analysis.
 *
 * @author Freeman
 */
public class AnalysisWarning {

    private final WarningType type;
    private final String message;
    private final String className;
    private final Optional<String> suggestion;

    public AnalysisWarning(WarningType type, String message, String className, Optional<String> suggestion) {
        this.type = type;
        this.message = message;
        this.className = className;
        this.suggestion = suggestion;
    }

    // Getter methods
    public WarningType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getClassName() {
        return className;
    }

    public Optional<String> getSuggestion() {
        return suggestion;
    }

    @Override
    public String toString() {
        return "AnalysisWarning{" + "type="
                + type + ", message='"
                + message + '\'' + ", className='"
                + className + '\'' + ", suggestion="
                + suggestion + '}';
    }
}
