package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * {@link ByteCodeUtil}
 */
class ByteCodeUtilTest {

    /**
     * {@link ByteCodeUtil#getDependencies(ByteCode)}
     */
    @Test
    @SneakyThrows
    void getDependencies_whenUsingLinkedHashSet() {

        var c = ClassLoader.getSystemResourceAsStream("java/util/LinkedHashSet.class");
        assertThat(c).isNotNull();

        var dependencies = ByteCodeUtil.getDependencies(ByteCode.of(c.readAllBytes()));

        assertThat(dependencies)
                .containsExactly(
                        "java.util.HashSet",
                        "java.util.SequencedSet",
                        "java.lang.Cloneable",
                        "java.io.Serializable",
                        "java.util.Collection",
                        "java.lang.Math",
                        "java.util.HashMap",
                        "java.util.LinkedHashSet",
                        "java.util.Spliterator",
                        "java.util.Spliterators",
                        "java.lang.IllegalArgumentException",
                        "java.lang.StringBuilder",
                        "java.util.LinkedHashMap",
                        "java.lang.Object",
                        "java.util.LinkedHashSet$1ReverseLinkedHashSetView",
                        "java.util.SequencedCollection");
    }

    /**
     * {@link ByteCodeUtil#getDependencies(ByteCode)}
     */
    @Test
    @SneakyThrows
    void getDependencies_whenUsingObjects() {

        var is = ClassLoader.getSystemResourceAsStream("java/util/Objects.class");
        assertThat(is).isNotNull();

        var dependencies1 = ByteCodeUtil.getDependencies(ByteCode.of(is.readAllBytes()));
        assertThat(dependencies1)
                .containsExactly(
                        "java.lang.Object",
                        "java.lang.AssertionError",
                        "java.util.Arrays",
                        "java.lang.String",
                        "java.util.Objects",
                        "java.lang.StringBuilder",
                        "java.lang.Class",
                        "java.lang.System",
                        "java.lang.Integer",
                        "java.util.Comparator",
                        "java.lang.NullPointerException",
                        "jdk.internal.vm.annotation.ForceInline",
                        "java.util.function.Supplier",
                        "jdk.internal.util.Preconditions");
    }

    /**
     * {@link ByteCodeUtil#listLibJarInPath(Path)}
     */
    @Test
    void listLibJarInPath_whenUsingJarFile() {
        var path = Path.of("src/test/resources/moego-server.jar");
        var libJars = ByteCodeUtil.listLibJarInPath(path);
        assertThat(libJars).isNotEmpty();
    }

    /**
     * {@link ByteCodeUtil#listLibJarInPath(Path)}
     */
    @Test
    void listLibJarInPath_whenUsingDir() {
        var path = Path.of("src/test/resources");
        var libJars = ByteCodeUtil.listLibJarInPath(path);
        assertThat(libJars).isNotEmpty();
    }
}
