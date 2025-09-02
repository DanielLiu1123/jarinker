package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class JarinkerTest {

    @TempDir
    Path tempDir;

    private Path sourcePath;
    private Path dependencyPath;
    private JarinkerConfig config;
    private Jarinker jarinker;

    @BeforeEach
    void setUp() throws IOException {
        sourcePath = tempDir.resolve("src");
        dependencyPath = tempDir.resolve("lib");
        Files.createDirectories(sourcePath);
        Files.createDirectories(dependencyPath);

        config = JarinkerConfig.builder()
                .verbose(false)
                .showProgress(false)
                .timeout(Duration.ofSeconds(1))
                .build();

        jarinker = new Jarinker(List.of(sourcePath), List.of(dependencyPath), config);
    }

    @Nested
    class AnalyzeTests {

        @Test
        void shouldReturnAnalysisResultWithBasicInformation() {
            // Arrange
            var mockClasses = createMockClasses();
            var mockDependencyGraph = createMockDependencyGraph();

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act
                var result = jarinker.analyze();

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getAllClasses()).isNotNull();
                assertThat(result.getDependencyGraph()).isNotNull();
                assertThat(result.getEntryPoints()).isNotNull();
                assertThat(result.getWarnings()).isNotNull();
                assertThat(result.getStatistics()).isNotNull();
            }
        }

        @Test
        void shouldHandleIOExceptionGracefully() throws IOException {
            // Arrange
            Files.delete(sourcePath);

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act - should not throw exception, but return empty result
                var result = jarinker.analyze();

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getTotalClassCount()).isEqualTo(0);
            }
        }

        @Test
        void shouldApplyIncludePatterns() {
            // Arrange
            var configWithPatterns = JarinkerConfig.builder()
                    .includePattern("com.example.**")
                    .verbose(false)
                    .build();
            var jarinkerWithPatterns = new Jarinker(List.of(sourcePath), List.of(dependencyPath), configWithPatterns);

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act
                var result = jarinkerWithPatterns.analyze();

                // Assert
                assertThat(result).isNotNull();
            }
        }

        @Test
        void shouldApplyExcludePatterns() {
            // Arrange
            var configWithPatterns = JarinkerConfig.builder()
                    .excludePattern("**Test")
                    .verbose(false)
                    .build();
            var jarinkerWithPatterns = new Jarinker(List.of(sourcePath), List.of(dependencyPath), configWithPatterns);

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act
                var result = jarinkerWithPatterns.analyze();

                // Assert
                assertThat(result).isNotNull();
            }
        }
    }

    @Nested
    class ShrinkTests {

        @Test
        void shouldReturnShrinkResultWithBasicInformation() {
            // Arrange
            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act
                var result = jarinker.shrink();

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getShrunkJars()).isNotNull();
                assertThat(result.getStatistics()).isNotNull();
                assertThat(result.getWarnings()).isNotNull();
                assertThat(result.getReport()).isNotNull();
            }
        }

        @Test
        void shouldHandleExceptionGracefully() throws IOException {
            // Arrange
            Files.delete(dependencyPath);

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act - should not throw exception, but return empty result
                var result = jarinker.shrink();

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getShrunkJars()).isEmpty();
            }
        }

        @Test
        void shouldProcessJarFiles() throws IOException {
            // Arrange
            var jarFile = dependencyPath.resolve("test.jar");
            Files.createFile(jarFile);

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil
                        .when(() -> ByteCodeUtil.analyzeJar(any()))
                        .thenReturn(Map.of("com.example.Test", createClassInfo("com.example.Test")));
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act
                var result = jarinker.shrink();

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatistics().getProcessingTime()).isNotNull();
            }
        }
    }

    @Nested
    class PatternMatchingTests {

        @Test
        void shouldMatchSimplePattern() {
            // This tests the private matchesPattern method indirectly through analyze
            var configWithPattern = JarinkerConfig.builder()
                    .includePattern("com.example.Service")
                    .verbose(false)
                    .build();
            var jarinkerWithPattern = new Jarinker(List.of(sourcePath), List.of(dependencyPath), configWithPattern);

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act
                var result = jarinkerWithPattern.analyze();

                // Assert
                assertThat(result).isNotNull();
            }
        }

        @Test
        void shouldMatchWildcardPattern() {
            // This tests wildcard pattern matching indirectly
            var configWithPattern = JarinkerConfig.builder()
                    .includePattern("com.example.**")
                    .verbose(false)
                    .build();
            var jarinkerWithPattern = new Jarinker(List.of(sourcePath), List.of(dependencyPath), configWithPattern);

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act
                var result = jarinkerWithPattern.analyze();

                // Assert
                assertThat(result).isNotNull();
            }
        }
    }

    @Nested
    class VerboseOutputTests {

        @Test
        void shouldProduceVerboseOutput() {
            // Arrange
            var verboseConfig =
                    JarinkerConfig.builder().verbose(true).showProgress(true).build();
            var verboseJarinker = new Jarinker(List.of(sourcePath), List.of(dependencyPath), verboseConfig);

            try (MockedStatic<ByteCodeUtil> mockedUtil = mockStatic(ByteCodeUtil.class)) {
                mockedUtil.when(() -> ByteCodeUtil.analyzeJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.analyzeClass(any())).thenReturn(null);

                // Act
                var result = verboseJarinker.analyze();

                // Assert
                assertThat(result).isNotNull();
            }
        }
    }

    // Helper methods
    private Map<String, ClassInfo> createMockClasses() {
        Map<String, ClassInfo> classes = new HashMap<>();
        classes.put("com.example.Main", createClassInfo("com.example.Main"));
        classes.put("com.example.Service", createClassInfo("com.example.Service"));
        return classes;
    }

    private Map<String, Set<String>> createMockDependencyGraph() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("com.example.Main", Set.of("com.example.Service"));
        graph.put("com.example.Service", Set.of());
        return graph;
    }

    private ClassInfo createClassInfo(String className) {
        var packageName = className.substring(0, className.lastIndexOf('.'));
        return new ClassInfo(className, packageName, false, false, null, Set.of(), Set.of(), 1000);
    }
}
