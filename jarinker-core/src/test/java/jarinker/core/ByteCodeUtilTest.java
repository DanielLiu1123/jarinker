package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;

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
                .contains(
                        "java.util.HashSet",
                        "java.util.Set",
                        "java.lang.Cloneable",
                        "java.io.Serializable",
                        "java.util.Collection",
                        "java.lang.Math",
                        "java.util.Spliterator",
                        "java.util.Spliterators");
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
                .contains(
                        "java.lang.Object",
                        "java.lang.AssertionError",
                        "java.util.Arrays",
                        "java.lang.String",
                        "java.util.Comparator",
                        "java.lang.NullPointerException",
                        "jdk.internal.vm.annotation.ForceInline",
                        "java.util.function.Supplier",
                        "jdk.internal.util.Preconditions");
    }
}
