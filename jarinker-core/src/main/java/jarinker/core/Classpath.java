package jarinker.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * {@link Classpath} represents a collection of dependencies bytecodes.
 *
 * @author Freeman
 * @since 2024/10/10
 */
@Getter
@EqualsAndHashCode
public final class Classpath {

    private final LinkedHashMap<String, ByteCode> dependencies;

    private Classpath(LinkedHashMap<String, ByteCode> dependencies) {
        this.dependencies = dependencies;
    }

    public static Classpath of(Collection<ByteCode> dependencies) {

        var dependencyMap = new LinkedHashMap<String, ByteCode>();
        for (var dependency : dependencies) {
            dependencyMap.put(dependency.className(), dependency);
        }

        return new Classpath(dependencyMap);
    }
}
