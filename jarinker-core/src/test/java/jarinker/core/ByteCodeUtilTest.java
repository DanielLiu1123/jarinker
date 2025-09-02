package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * {@link ByteCodeUtil}
 */
class ByteCodeUtilTest {

    @TempDir
    Path tempDir;

    @Nested
    class AnalyzeClassTests {

        @Test
        void shouldReturnDummyClassInfoForNonExistentFile() {
            // Arrange
            var nonExistentFile = tempDir.resolve("NonExistent.class");

            // Act
            var result = ByteCodeUtil.analyzeClass(nonExistentFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getClassName()).isEqualTo("NonExistent");
            assertThat(result.getSize()).isEqualTo(0);
        }

        @Test
        void shouldAnalyzeSimpleClass() throws IOException {
            // Arrange
            var classFile = createSimpleClassFile("com.example.TestClass");

            // Act
            var result = ByteCodeUtil.analyzeClass(classFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getClassName()).isEqualTo("com.example.TestClass");
            assertThat(result.getPackageName()).isEqualTo("com.example");
            assertThat(result.isInterface()).isFalse();
            assertThat(result.isAbstract()).isFalse();
            assertThat(result.getSuperClass()).isEqualTo("java.lang.Object");
            assertThat(result.getSize()).isGreaterThan(0);
        }

        @Test
        void shouldAnalyzeInterface() throws IOException {
            // Arrange
            var interfaceFile = createInterfaceFile("com.example.TestInterface");

            // Act
            var result = ByteCodeUtil.analyzeClass(interfaceFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getClassName()).isEqualTo("com.example.TestInterface");
            assertThat(result.isInterface()).isTrue();
            assertThat(result.isAbstract()).isTrue();
        }

        @Test
        void shouldReturnDummyClassInfoForInvalidClassFile() throws IOException {
            // Arrange
            var invalidFile = tempDir.resolve("Invalid.class");
            Files.write(invalidFile, "invalid bytecode".getBytes());

            // Act
            var result = ByteCodeUtil.analyzeClass(invalidFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getClassName()).isEqualTo("Invalid");
            assertThat(result.getSize()).isEqualTo(0);
        }
    }

    @Nested
    class AnalyzeJarTests {

        @Test
        void shouldReturnEmptyMapForNonExistentJar() {
            // Arrange
            var nonExistentJar = tempDir.resolve("nonexistent.jar");

            // Act & Assert
            assertThatThrownBy(() -> ByteCodeUtil.analyzeJar(nonExistentJar))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to analyze JAR");
        }

        @Test
        void shouldAnalyzeEmptyJar() throws IOException {
            // Arrange
            var emptyJar = createEmptyJar();

            // Act
            var result = ByteCodeUtil.analyzeJar(emptyJar);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void shouldAnalyzeJarWithClasses() throws IOException {
            // Arrange
            var jarWithClasses = createJarWithClasses();

            // Act
            var result = ByteCodeUtil.analyzeJar(jarWithClasses);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).containsKey("com.example.TestClass");

            var classInfo = result.get("com.example.TestClass");
            assertThat(classInfo.getClassName()).isEqualTo("com.example.TestClass");
            assertThat(classInfo.getPackageName()).isEqualTo("com.example");
        }
    }

    @Nested
    class ExtractDependenciesTests {

        @Test
        void shouldExtractSuperClassDependency() {
            // Arrange
            var classInfo = new ClassInfo(
                    "com.example.Child", "com.example", false, false, "com.example.Parent", Set.of(), Set.of(), 1000);

            // Act
            var dependencies = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            assertThat(dependencies).contains("com.example.Parent");
        }

        @Test
        void shouldReturnEmptySetForNoDependencies() {
            // Arrange
            var classInfo = new ClassInfo(
                    "com.example.SimpleClass", "com.example", false, false, null, Set.of(), Set.of(), 1000);

            // Act
            var dependencies = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            assertThat(dependencies).isEmpty();
        }
    }

    // Helper methods
    private Path createSimpleClassFile(String className) throws IOException {
        var classWriter = new ClassWriter(0);
        var internalName = className.replace('.', '/');

        classWriter.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        classWriter.visitEnd();

        var bytecode = classWriter.toByteArray();
        var classFile = tempDir.resolve(className.substring(className.lastIndexOf('.') + 1) + ".class");
        Files.write(classFile, bytecode);

        return classFile;
    }

    private Path createInterfaceFile(String interfaceName) throws IOException {
        var classWriter = new ClassWriter(0);
        var internalName = interfaceName.replace('.', '/');

        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                internalName,
                null,
                "java/lang/Object",
                null);
        classWriter.visitEnd();

        var bytecode = classWriter.toByteArray();
        var interfaceFile = tempDir.resolve(interfaceName.substring(interfaceName.lastIndexOf('.') + 1) + ".class");
        Files.write(interfaceFile, bytecode);

        return interfaceFile;
    }

    private Path createEmptyJar() throws IOException {
        var jarFile = tempDir.resolve("empty.jar");
        try (var jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            // Empty JAR
        }
        return jarFile;
    }

    private Path createJarWithClasses() throws IOException {
        var jarFile = tempDir.resolve("withclasses.jar");

        // Create a simple class bytecode
        var classWriter = new ClassWriter(0);
        classWriter.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "com/example/TestClass", null, "java/lang/Object", null);
        classWriter.visitEnd();
        var bytecode = classWriter.toByteArray();

        try (var jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            var entry = new JarEntry("com/example/TestClass.class");
            jos.putNextEntry(entry);
            jos.write(bytecode);
            jos.closeEntry();
        }

        return jarFile;
    }
}
