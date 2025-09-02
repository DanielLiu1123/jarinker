package jarinker.core;

import java.util.LinkedHashSet;
import java.util.jar.JarFile;

/**
 * {@link LibJar} represents a jar file and its classes.
 *
 * @author Freeman
 * @since 2024/10/13
 */
public record LibJar(String name, LinkedHashSet<ByteCode> classes) {

    /**
     * Factory method to create a {@link LibJar} instance from a jar file.
     *
     * @param jarFile jar file
     * @return {@link LibJar} instance
     */
    public static LibJar of(JarFile jarFile) {
        var classList = ByteCodeUtil.listByteCodeInJar(jarFile);
        return new LibJar(jarFile.getName(), classList);
    }
}
