package jarinker.core;

import java.util.LinkedHashSet;
import java.util.jar.JarFile;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * {@link LibJar} represents a jar file and its classes.
 *
 * @author Freeman
 * @since 2024/10/13
 */
@Getter
@EqualsAndHashCode
public final class LibJar {

    private final String name;
    private final LinkedHashSet<ByteCode> classes;

    private LibJar(String name, LinkedHashSet<ByteCode> classes) {
        this.name = name;
        this.classes = classes;
    }

    /**
     * Factory method to create a {@link LibJar} instance from a jar file.
     *
     * @param jarFile jar file
     * @return {@link LibJar} instance
     */
    public static LibJar of(JarFile jarFile) {
        assert jarFile != null;

        var classList = ByteCodeUtil.listByteCodeInJar(jarFile);
        return new LibJar(jarFile.getName(), classList);
    }
}
