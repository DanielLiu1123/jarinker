package jarinker.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.objectweb.asm.ClassReader;

/**
 * {@link ByteCode} represents a bytecode and its class name.
 *
 * @author Freeman
 * @since 2024/10/10
 */
@Getter
@EqualsAndHashCode
public final class ByteCode {

    private final String className;
    private final byte[] byteCode;

    private ByteCode(String className, byte[] byteCode) {
        this.className = className;
        this.byteCode = byteCode;
    }

    /**
     * Factory method to create a {@link ByteCode} instance from class name and bytecode.
     *
     * @param className class name
     * @param byteCode  bytecode
     * @return {@link ByteCode} instance
     */
    public static ByteCode of(String className, byte[] byteCode) {
        assert className != null;
        assert byteCode != null;

        return new ByteCode(className, byteCode);
    }

    /**
     * Factory method to create a {@link ByteCode} instance from bytecode.
     *
     * @param byteCode bytecode
     * @return {@link ByteCode} instance
     */
    public static ByteCode of(byte[] byteCode) {
        assert byteCode != null;

        var cr = new ClassReader(byteCode);
        var className = cr.getClassName().replace('/', '.');
        return of(className, byteCode);
    }
}
