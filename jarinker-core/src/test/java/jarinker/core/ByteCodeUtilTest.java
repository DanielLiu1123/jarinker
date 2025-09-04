package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.HashMap;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ByteCodeUtilTest {

    @Nested
    class ExtractDependenciesTests {

        @Test
        void shouldReturnEmptySetWhenClassHasNoDependencies() {
            // Arrange
            var classInfo = createSimpleClassInfo("com.example.SimpleClass");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            var expected = Set.of("java.lang.Object");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldExtractSuperclassDependency() {
            // Arrange
            var classInfo = createClassInfoWithSuperclass("com.example.Child", "com.example.Parent");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            var expected = Set.of("com.example.Parent");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldExtractInterfaceDependencies() {
            // Arrange
            var classInfo = createClassInfoWithInterface("com.example.Implementation", "com.example.Interface");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            var expected = Set.of("java.lang.Object", "com.example.Interface");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldExtractFieldTypeDependencies() {
            // Arrange
            var classInfo = createClassInfoWithField("com.example.ClassWithField", "com.example.FieldType");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            var expected = Set.of("java.lang.Object", "com.example.FieldType");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldExtractMethodParameterAndReturnTypeDependencies() {
            // Arrange
            var classInfo = createClassInfoWithMethod(
                    "com.example.ClassWithMethod", "com.example.ParamType", "com.example.ReturnType");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            var expected = Set.of("java.lang.Object", "com.example.ParamType", "com.example.ReturnType");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldFilterOutSelfReference() {
            // Arrange
            var classInfo = createSimpleClassInfo("com.example.SelfReferencing");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            assertThat(actual).doesNotContain("com.example.SelfReferencing");
        }

        @Test
        void shouldExtractDependenciesIncludingPrimitiveTypes() {
            // Arrange
            var classInfo = createClassInfoWithPrimitiveField("com.example.ClassWithPrimitive");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            var expected = Set.of("java.lang.Object", "int", "boolean");
            assertThat(actual).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "I", "Z", "B", "C", "S", "J", "F", "D", "void"})
        void shouldFilterOutInvalidClassNames(String invalidClassName) {
            // Arrange
            var classInfo = createSimpleClassInfo("com.example.TestClass");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert - 验证无效类名不会出现在结果中
            // 这些是JVM内部表示的原始类型和无效类名，应该被过滤掉
            assertThat(actual).doesNotContain(invalidClassName);
        }

        @Test
        void shouldExtractMultipleDependencyTypes() {
            // Arrange
            var classInfo = createComplexClassInfo("com.example.ComplexClass");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            var expected = Set.of(
                    "com.example.SuperClass",
                    "com.example.Interface1",
                    "com.example.Interface2",
                    "com.example.FieldType",
                    "com.example.ParamType",
                    "com.example.ReturnType");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldHandleClassWithNoSuperclass() {
            // Arrange - Object类没有显式的超类
            var classInfo = createSimpleClassInfo("java.lang.Object");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert - Object类应该没有依赖（除了可能的内部引用）
            assertThat(actual).doesNotContain("java.lang.Object");
        }

        @Test
        void shouldExtractDependenciesFromMultipleInterfaces() {
            // Arrange
            var classInfo = createClassInfoWithMultipleInterfaces("com.example.MultiImpl");

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            // Assert
            var expected = Set.of(
                    "java.lang.Object", "com.example.Interface1", "com.example.Interface2", "com.example.Interface3");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldExtractDependenciesFromHashMapClass() {
            // Arrange - 直接分析 HashMap 类本身
            var classInfo = createRealHashMapClassInfo();

            // Act
            var actual = ByteCodeUtil.extractDependencies(classInfo);

            printHashMapDependencies(actual);

            assertThat(actual)
                    .contains(
                            "java.io.IOException",
                            "java.io.InvalidObjectException",
                            "java.io.ObjectInputStream",
                            "java.io.Serializable",
                            "java.lang.reflect.ParameterizedType",
                            "java.lang.reflect.Type",
                            "java.util.function.BiConsumer",
                            "java.util.function.BiFunction",
                            "java.util.function.Consumer",
                            "java.util.function.Function",
                            "jdk.internal.access.SharedSecrets");
            assertThat(actual).doesNotContain("java.util.HashMap");
        }
    }

    // Helper methods for creating test ClassInfo instances

    private ClassInfo createSimpleClassInfo(String className) {
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

    private ClassInfo createClassInfoWithSuperclass(String className, String superClassName) {
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of(superClassName);

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

    private ClassInfo createClassInfoWithInterface(String className, String interfaceName) {
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");
        var interfaceDesc = ClassDesc.of(interfaceName);

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder
                    .withFlags(ClassFile.ACC_PUBLIC)
                    .withSuperclass(superClassDesc)
                    .withInterfaceSymbols(interfaceDesc);
        });

        try {
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassInfo for " + className, e);
        }
    }

    private ClassInfo createClassInfoWithField(String className, String fieldTypeName) {
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");
        var fieldTypeDesc = ClassDesc.of(fieldTypeName);

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder
                    .withFlags(ClassFile.ACC_PUBLIC)
                    .withSuperclass(superClassDesc)
                    .withField("field", fieldTypeDesc, ClassFile.ACC_PRIVATE);
        });

        try {
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassInfo for " + className, e);
        }
    }

    private ClassInfo createClassInfoWithMethod(String className, String paramTypeName, String returnTypeName) {
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");
        var paramTypeDesc = ClassDesc.of(paramTypeName);
        var returnTypeDesc = ClassDesc.of(returnTypeName);
        var methodTypeDesc = MethodTypeDesc.of(returnTypeDesc, paramTypeDesc);

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder
                    .withFlags(ClassFile.ACC_PUBLIC)
                    .withSuperclass(superClassDesc)
                    .withMethod("testMethod", methodTypeDesc, ClassFile.ACC_PUBLIC, methodBuilder -> {
                        methodBuilder.withCode(codeBuilder -> {
                            codeBuilder.aload(1).areturn();
                        });
                    });
        });

        try {
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassInfo for " + className, e);
        }
    }

    private ClassInfo createClassInfoWithPrimitiveField(String className) {
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder
                    .withFlags(ClassFile.ACC_PUBLIC)
                    .withSuperclass(superClassDesc)
                    .withField("intField", ClassDesc.of("int"), ClassFile.ACC_PRIVATE)
                    .withField("booleanField", ClassDesc.of("boolean"), ClassFile.ACC_PRIVATE);
        });

        try {
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassInfo for " + className, e);
        }
    }

    private ClassInfo createComplexClassInfo(String className) {
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("com.example.SuperClass");
        var interface1Desc = ClassDesc.of("com.example.Interface1");
        var interface2Desc = ClassDesc.of("com.example.Interface2");
        var fieldTypeDesc = ClassDesc.of("com.example.FieldType");
        var paramTypeDesc = ClassDesc.of("com.example.ParamType");
        var returnTypeDesc = ClassDesc.of("com.example.ReturnType");
        var methodTypeDesc = MethodTypeDesc.of(returnTypeDesc, paramTypeDesc);

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder
                    .withFlags(ClassFile.ACC_PUBLIC)
                    .withSuperclass(superClassDesc)
                    .withInterfaceSymbols(interface1Desc, interface2Desc)
                    .withField("field", fieldTypeDesc, ClassFile.ACC_PRIVATE)
                    .withMethod("testMethod", methodTypeDesc, ClassFile.ACC_PUBLIC, methodBuilder -> {
                        methodBuilder.withCode(codeBuilder -> {
                            codeBuilder.aload(1).areturn();
                        });
                    });
        });

        try {
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassInfo for " + className, e);
        }
    }

    private ClassInfo createClassInfoWithMultipleInterfaces(String className) {
        var classDesc = ClassDesc.of(className);
        var superClassDesc = ClassDesc.of("java.lang.Object");
        var interface1Desc = ClassDesc.of("com.example.Interface1");
        var interface2Desc = ClassDesc.of("com.example.Interface2");
        var interface3Desc = ClassDesc.of("com.example.Interface3");

        var bytecode = ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder
                    .withFlags(ClassFile.ACC_PUBLIC)
                    .withSuperclass(superClassDesc)
                    .withInterfaceSymbols(interface1Desc, interface2Desc, interface3Desc);
        });

        try {
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassInfo for " + className, e);
        }
    }

    private ClassInfo createRealHashMapClassInfo() {
        try {
            // 获取 HashMap 类的字节码
            var hashMapClass = HashMap.class;
            var resourceName = hashMapClass.getName().replace('.', '/') + ".class";

            // 从系统类加载器获取字节码
            byte[] bytecode;
            try (var inputStream = ClassLoader.getSystemResourceAsStream(resourceName)) {
                if (inputStream == null) {
                    throw new RuntimeException("Cannot find HashMap class file");
                }
                bytecode = inputStream.readAllBytes();
            }

            // 解析字节码
            ClassModel classModel = ClassFile.of().parse(bytecode);
            return ClassInfo.of(classModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassInfo for HashMap", e);
        }
    }

    private void printHashMapDependencies(Set<String> dependencies) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("HashMap Class Dependencies Analysis");
        System.out.println("=".repeat(80));
        System.out.println("Total dependencies found: " + dependencies.size());
        System.out.println();

        // 按包名分组依赖
        var dependenciesByPackage = dependencies.stream()
                .sorted()
                .collect(java.util.stream.Collectors.groupingBy(
                        className -> {
                            var lastDot = className.lastIndexOf('.');
                            return lastDot > 0 ? className.substring(0, lastDot) : "<default>";
                        },
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        dependenciesByPackage.forEach((packageName, classes) -> {
            System.out.println("📦 " + packageName + " (" + classes.size() + " classes)");
            classes.forEach(className -> {
                var simpleName = className.substring(className.lastIndexOf('.') + 1);
                System.out.println("   ├─ " + simpleName);
            });
            System.out.println();
        });

        System.out.println("=".repeat(80));
        System.out.println();
    }
}
