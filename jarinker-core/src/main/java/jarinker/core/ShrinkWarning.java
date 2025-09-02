package jarinker.core;

import java.util.Optional;

/**
 * Warning generated during shrinking operation.
 *
 * @author Freeman
 */
public class ShrinkWarning {

    private final WarningType type;
    private final String message;
    private final String jarName;
    private final Optional<String> suggestion;

    public ShrinkWarning(WarningType type, String message, String jarName, Optional<String> suggestion) {
        this.type = type;
        this.message = message;
        this.jarName = jarName;
        this.suggestion = suggestion;
    }

    // Getter methods
    public WarningType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getJarName() {
        return jarName;
    }

    public Optional<String> getSuggestion() {
        return suggestion;
    }

    @Override
    public String toString() {
        return "ShrinkWarning{" + "type="
                + type + ", message='"
                + message + '\'' + ", jarName='"
                + jarName + '\'' + ", suggestion="
                + suggestion + '}';
    }
}
