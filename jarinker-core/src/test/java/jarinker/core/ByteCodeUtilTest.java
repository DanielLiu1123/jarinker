package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link ByteCodeUtil}
 */
class ByteCodeUtilTest {

    @TempDir
    Path tempDir;

    @Nested
    class AnalyzeClassTests {

        @Test
        void shouldReturnNullForNonExistentFile() {
            // Arrange
            var nonExistentFile = tempDir.resolve("NonExistent.class");

            // Act
            var result = ByteCodeUtil.readClass(nonExistentFile);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        void shouldAnalyzeSimpleClass() throws IOException {
            // Arrange
            var classFile = createSimpleClassFile("com.example.TestClass");

            // Act
            var result = ByteCodeUtil.readClass(classFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getClassName()).isEqualTo("com.example.TestClass");

            var model = result.getModel();
            assertThat(model.thisClass().asInternalName().replace('/', '.')).isEqualTo("com.example.TestClass");
            assertThat(model.flags().has(java.lang.reflect.AccessFlag.INTERFACE))
                    .isFalse();
            assertThat(model.flags().has(java.lang.reflect.AccessFlag.ABSTRACT)).isFalse();
            assertThat(model.superclass().get().asInternalName().replace('/', '.'))
                    .isEqualTo("java.lang.Object");
        }

        @Test
        void shouldAnalyzeInterface() throws IOException {
            // Arrange
            var interfaceFile = createInterfaceFile("com.example.TestInterface");

            // Act
            var result = ByteCodeUtil.readClass(interfaceFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getClassName()).isEqualTo("com.example.TestInterface");

            var model = result.getModel();
            assertThat(model.flags().has(java.lang.reflect.AccessFlag.INTERFACE))
                    .isTrue();
            assertThat(model.flags().has(java.lang.reflect.AccessFlag.ABSTRACT)).isTrue();
        }

        @Test
        void shouldReturnNullForInvalidClassFile() throws IOException {
            // Arrange
            var invalidFile = tempDir.resolve("Invalid.class");
            Files.write(invalidFile, "invalid bytecode".getBytes());

            // Act
            var result = ByteCodeUtil.readClass(invalidFile);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    class AnalyzeJarTests {

        @Test
        void shouldReturnEmptyMapForNonExistentJar() {
            // Arrange
            var nonExistentJar = tempDir.resolve("nonexistent.jar");

            // Act & Assert
            assertThatThrownBy(() -> ByteCodeUtil.readJar(nonExistentJar))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to analyze JAR");
        }

        @Test
        void shouldAnalyzeEmptyJar() throws IOException {
            // Arrange
            var emptyJar = createEmptyJar();

            // Act
            var result = ByteCodeUtil.readJar(emptyJar);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void shouldAnalyzeJarWithClasses() throws IOException {
            // Arrange
            var jarWithClasses = createJarWithClasses();

            // Act
            var result = ByteCodeUtil.readJar(jarWithClasses);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).containsKey("com.example.TestClass");

            var classInfo = result.get("com.example.TestClass");
            assertThat(classInfo.getClassName()).isEqualTo("com.example.TestClass");
        }
    }

    @Nested
    class ExtractDependenciesTests {

        @Test
        void shouldExtractSuperClassDependency() {
            // Arrange
            var classDesc = ClassDesc.of("com.example.Child");
            var superClassDesc = ClassDesc.of("com.example.Parent");

            var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
                classBuilder.withFlags(ClassFile.ACC_PUBLIC).withSuperclass(superClassDesc);
            });

            try {
                ClassModel classModel = ClassFile.of().parse(bytecode);
                var classInfo = ClassInfo.of(classModel);

                // Act
                var dependencies = ByteCodeUtil.extractDependencies(classInfo);

                // Assert
                assertThat(dependencies).contains("com.example.Parent");
            } catch (Exception e) {
                fail("Failed to create test ClassModel", e);
            }
        }

        @Test
        void shouldReturnEmptySetForNoDependencies() {
            // Arrange
            var classDesc = ClassDesc.of("com.example.SimpleClass");
            var superClassDesc = ClassDesc.of("java.lang.Object");

            var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
                classBuilder.withFlags(ClassFile.ACC_PUBLIC).withSuperclass(superClassDesc);
            });

            try {
                ClassModel classModel = ClassFile.of().parse(bytecode);
                var classInfo = ClassInfo.of(classModel);

                // Act
                var dependencies = ByteCodeUtil.extractDependencies(classInfo);

                // Assert
                assertThat(dependencies).contains("java.lang.Object");
            } catch (Exception e) {
                fail("Failed to create test ClassModel", e);
            }
        }

        @Test
        void shouldExtractAllTypesOfDependencies() {
            // Arrange
            var classDesc = ClassDesc.of("com.example.ComplexClass");
            var superClassDesc = ClassDesc.of("com.example.BaseClass");
            var interfaceDesc = ClassDesc.of("com.example.MyInterface");

            var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
                classBuilder
                        .withFlags(ClassFile.ACC_PUBLIC)
                        .withSuperclass(superClassDesc)
                        .withInterfaceSymbols(interfaceDesc)
                        .withField("myField", ClassDesc.of("com.example.FieldType"), ClassFile.ACC_PRIVATE)
                        .withMethod(
                                "myMethod",
                                MethodTypeDesc.of(
                                        ClassDesc.of("com.example.ReturnType"), ClassDesc.of("com.example.ParamType")),
                                ClassFile.ACC_PUBLIC,
                                methodBuilder -> {
                                    methodBuilder.withCode(codeBuilder -> {
                                        codeBuilder
                                                .new_(ClassDesc.of("com.example.NewObjectType"))
                                                .dup()
                                                .invokespecial(
                                                        ClassDesc.of("com.example.NewObjectType"),
                                                        ConstantDescs.INIT_NAME,
                                                        MethodTypeDesc.of(ConstantDescs.CD_void))
                                                .return_();
                                    });
                                });
            });

            try {
                ClassModel classModel = ClassFile.of().parse(bytecode);
                var classInfo = ClassInfo.of(classModel);

                // Act
                var dependencies = ByteCodeUtil.extractDependencies(classInfo);

                // Assert - should extract dependencies from various sources
                assertThat(dependencies)
                        .contains(
                                "com.example.BaseClass", // superclass
                                "com.example.MyInterface", // interface
                                "com.example.FieldType", // field type
                                "com.example.ReturnType", // method return type
                                "com.example.ParamType", // method parameter type
                                "com.example.NewObjectType" // new object in method body
                                );

                // Should not contain the class itself
                assertThat(dependencies).doesNotContain("com.example.ComplexClass");
            } catch (Exception e) {
                fail("Failed to create test ClassModel", e);
            }
        }
    }

    // Helper methods
    private Path createSimpleClassFile(String className) throws IOException {
        var internalName = className.replace('.', '/');
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC).withSuperclass(superClassDesc);
        });

        var classFile = tempDir.resolve(className.substring(className.lastIndexOf('.') + 1) + ".class");
        Files.write(classFile, bytecode);

        return classFile;
    }

    private Path createInterfaceFile(String interfaceName) throws IOException {
        var classDesc = ClassDesc.of(interfaceName);
        var superClassDesc = ClassDesc.of("java.lang.Object");

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder
                    .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_INTERFACE | ClassFile.ACC_ABSTRACT)
                    .withSuperclass(superClassDesc);
        });

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

        // Create a simple class bytecode using JDK Class File API
        var classDesc = ClassDesc.of("com.example.TestClass");
        var superClassDesc = ClassDesc.of("java.lang.Object");

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC).withSuperclass(superClassDesc);
        });

        try (var jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            var entry = new JarEntry("com/example/TestClass.class");
            jos.putNextEntry(entry);
            jos.write(bytecode);
            jos.closeEntry();
        }

        return jarFile;
    }
}
