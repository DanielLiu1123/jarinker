package jarinker.core;

import java.util.LinkedHashSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * {@link Dep} represents a bytecode and its dependencies.
 *
 * @author Freeman
 * @since 2024/10/11
 */
@Getter
@EqualsAndHashCode
public final class Dep {
    private final ByteCode byteCode;
    /**
     * All dependencies of the bytecode, including Java internal classes, not including transitive dependencies,
     */
    private final LinkedHashSet<ByteCode> dependencies;

    private Dep(ByteCode byteCode, LinkedHashSet<ByteCode> dependencies) {
        this.byteCode = byteCode;
        this.dependencies = dependencies;
    }

    /**
     * Resolve a bytecode's dependencies in a specific classpath.
     *
     * @param byteCode  bytecode
     * @param classpath classpath
     * @return dependencies of the bytecode
     */
    public static Dep of(ByteCode byteCode, Classpath classpath) {
        assert byteCode != null;
        assert classpath != null;

        var allDependencies = ByteCodeUtil.getDependencies(byteCode);

        var deps = new LinkedHashSet<ByteCode>();

        var dependencies = classpath.getDependencies();
        for (var dep : allDependencies) {
            var bc = dependencies.get(dep);
            if (bc != null) {
                deps.add(bc);
            }
        }

        return new Dep(byteCode, deps);
    }

    /**
     * Get external dependencies of the bytecode, excluding Java internal classes.
     *
     * @return external dependencies of the bytecode, excluding Java internal classes.
     */
    public LinkedHashSet<ByteCode> getExternalDependencies() {
        var result = new LinkedHashSet<ByteCode>();
        for (var dep : dependencies) {
            if (!isJavaInternalClass(dep.getClassName())) {
                result.add(dep);
            }
        }
        return result;
    }

    private static boolean isJavaInternalClass(String className) {
        return className.startsWith("java.") || className.startsWith("javax.");
    }
}
