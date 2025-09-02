package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * {@link ByteCode}
 */
class ByteCodeTest {

    /**
     * {@link ByteCode#of(byte[])}
     */
    @Test
    @SneakyThrows
    void of_whenPassingByteArray_thenCanGetClassName() {

        var clazz = ClassLoader.getSystemResourceAsStream("java/util/Objects.class");
        assertThat(clazz).isNotNull();

        var byteCode = ByteCode.of(clazz.readAllBytes());
        assertThat(byteCode.className()).isEqualTo("java.util.Objects");
        assertThat(byteCode.byteCode()).isNotEmpty();
    }
}
