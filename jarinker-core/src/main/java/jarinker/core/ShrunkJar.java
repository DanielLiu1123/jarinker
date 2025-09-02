package jarinker.core;

import java.nio.file.Path;

/**
 * Information about a shrunk JAR file.
 *
 * @author Freeman
 */
public class ShrunkJar {

    private final String originalName;
    private final String shrunkName;
    private final long originalSize;
    private final long shrunkSize;
    private final int originalClassCount;
    private final int shrunkClassCount;
    private final Path outputPath;

    public ShrunkJar(
            String originalName,
            String shrunkName,
            long originalSize,
            long shrunkSize,
            int originalClassCount,
            int shrunkClassCount,
            Path outputPath) {
        this.originalName = originalName;
        this.shrunkName = shrunkName;
        this.originalSize = originalSize;
        this.shrunkSize = shrunkSize;
        this.originalClassCount = originalClassCount;
        this.shrunkClassCount = shrunkClassCount;
        this.outputPath = outputPath;
    }

    // Getter methods
    public String getOriginalName() {
        return originalName;
    }

    public String getShrunkName() {
        return shrunkName;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public long getShrunkSize() {
        return shrunkSize;
    }

    public int getOriginalClassCount() {
        return originalClassCount;
    }

    public int getShrunkClassCount() {
        return shrunkClassCount;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public long getSizeSaved() {
        return originalSize - shrunkSize;
    }

    public double getShrinkRatio() {
        return originalSize == 0 ? 0.0 : (double) getSizeSaved() / originalSize;
    }

    @Override
    public String toString() {
        return "ShrunkJar{" + "originalName='"
                + originalName + '\'' + ", shrunkName='"
                + shrunkName + '\'' + ", originalSize="
                + originalSize + ", shrunkSize="
                + shrunkSize + ", originalClassCount="
                + originalClassCount + ", shrunkClassCount="
                + shrunkClassCount + ", outputPath="
                + outputPath + ", sizeSaved="
                + getSizeSaved() + ", shrinkRatio="
                + String.format("%.2f%%", getShrinkRatio() * 100) + '}';
    }
}
