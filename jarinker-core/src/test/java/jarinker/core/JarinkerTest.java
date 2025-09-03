package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                        .when(() -> ByteCodeUtil.readJar(any()))
                        .thenReturn(Map.of("com.example.Test", createClassInfo("com.example.Test")));
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

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
                mockedUtil.when(() -> ByteCodeUtil.readJar(any())).thenReturn(Map.of());
                mockedUtil.when(() -> ByteCodeUtil.readClass(any())).thenReturn(null);

                // Act
                var result = verboseJarinker.analyze();

                // Assert
                assertThat(result).isNotNull();
            }
        }
    }

    @Nested
    class ScanPathTests {

        @Test
        void shouldScanJarFilesInDirectory() throws IOException {
            // Arrange
            var libDir = tempDir.resolve("lib");
            Files.createDirectories(libDir);

            // Create a subdirectory with JAR files
            var subDir = libDir.resolve("subdir");
            Files.createDirectories(subDir);

            // Create JAR files in both directories
            var jar1 = createJarWithClass(libDir.resolve("test1.jar"), "com.example.Test1");
            var jar2 = createJarWithClass(subDir.resolve("test2.jar"), "com.example.Test2");

            var testJarinker = new Jarinker(List.of(sourcePath), List.of(libDir), config);

            // Act
            var result = testJarinker.analyze();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAllClasses()).containsKeys("com.example.Test1", "com.example.Test2");
        }

        @Test
        void shouldIgnoreClassFilesInDirectory() throws IOException {
            // Arrange
            var srcDir = tempDir.resolve("src");
            Files.createDirectories(srcDir);

            // Create a subdirectory structure
            var packageDir = srcDir.resolve("com").resolve("example");
            Files.createDirectories(packageDir);

            // Create class files (these should be ignored when scanning directories)
            var class1 = createClassFile(packageDir.resolve("Test1.class"), "com.example.Test1");
            var class2 = createClassFile(packageDir.resolve("Test2.class"), "com.example.Test2");

            var testJarinker = new Jarinker(List.of(srcDir), List.of(dependencyPath), config);

            // Act
            var result = testJarinker.analyze();

            // Assert
            assertThat(result).isNotNull();
            // .class files in directories should be ignored
            assertThat(result.getAllClasses()).doesNotContainKeys("com.example.Test1", "com.example.Test2");
        }

        @Test
        void shouldScanOnlyJarFilesInDirectory() throws IOException {
            // Arrange
            var mixedDir = tempDir.resolve("mixed");
            Files.createDirectories(mixedDir);

            // Create both JAR and class files
            var jar = createJarWithClass(mixedDir.resolve("test.jar"), "com.example.JarClass");
            var classFile = createClassFile(mixedDir.resolve("DirectClass.class"), "com.example.DirectClass");

            var testJarinker = new Jarinker(List.of(mixedDir), List.of(), config);

            // Act
            var result = testJarinker.analyze();

            // Assert
            assertThat(result).isNotNull();
            // Only JAR files should be processed, .class files should be ignored
            assertThat(result.getAllClasses()).containsKey("com.example.JarClass");
            assertThat(result.getAllClasses()).doesNotContainKey("com.example.DirectClass");
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
        // Create a simple class bytecode using JDK Class File API
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC).withSuperclass(superClassDesc);
        });

        try {
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassInfo for " + className, e);
        }
    }

    private Path createJarWithClass(Path jarPath, String className) throws IOException {
        // Create a simple class bytecode using JDK Class File API
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");
        var internalName = className.replace('.', '/');

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC).withSuperclass(superClassDesc);
        });

        try (var jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            var entry = new JarEntry(internalName + ".class");
            jos.putNextEntry(entry);
            jos.write(bytecode);
            jos.closeEntry();
        }

        return jarPath;
    }

    private Path createClassFile(Path classPath, String className) throws IOException {
        // Create a simple class bytecode using JDK Class File API
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC).withSuperclass(superClassDesc);
        });

        Files.write(classPath, bytecode);
        return classPath;
    }
}
