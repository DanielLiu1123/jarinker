package jarinker.core;

import org.objectweb.asm.ClassReader;

/**
 * {@link ByteCode} represents a bytecode and its class name.
 *
 * @author Freeman
 * @since 2024/10/10
 */
public record ByteCode(String className, byte[] byteCode) {

    /**
     * Factory method to create a {@link ByteCode} instance from class name and bytecode.
     *
     * @param className class name
     * @param byteCode  bytecode
     * @return {@link ByteCode} instance
     */
    public static ByteCode of(String className, byte[] byteCode) {
        return new ByteCode(className, byteCode);
    }

    /**
     * Factory method to create a {@link ByteCode} instance from bytecode.
     *
     * @param byteCode bytecode
     * @return {@link ByteCode} instance
     */
    public static ByteCode of(byte[] byteCode) {
        var cr = new ClassReader(byteCode);
        var className = cr.getClassName().replace('/', '.');
        return of(className, byteCode);
    }
}
