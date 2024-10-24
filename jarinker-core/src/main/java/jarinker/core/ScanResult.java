package jarinker.core;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.jar.JarFile;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author Freeman
 * @since 2024/10/16
 */
@Getter
@EqualsAndHashCode
public class ScanResult {

    private final LinkedHashSet<ByteCode> classFiles;
    private final Map<JarFile, LinkedHashSet<ByteCode>> sourceMap;
    private final LinkedHashSet<ByteCode> allClasses;

    private ScanResult(
            LinkedHashSet<ByteCode> classFiles,
            Map<JarFile, LinkedHashSet<ByteCode>> sourceMap,
            LinkedHashSet<ByteCode> allClasses) {
        this.classFiles = classFiles;
        this.sourceMap = sourceMap;
        this.allClasses = allClasses;
    }

    public static ScanResult of(
            LinkedHashSet<ByteCode> classFiles,
            Map<JarFile, LinkedHashSet<ByteCode>> sourceMap,
            LinkedHashSet<ByteCode> allClasses) {
        assert classFiles != null && sourceMap != null && allClasses != null;
        return new ScanResult(classFiles, sourceMap, allClasses);
    }
}
