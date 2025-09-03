package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultStrategyTest {

    private DefaultStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DefaultStrategy();
    }

    @Nested
    class BasicPropertiesTests {

        @Test
        void shouldReturnCorrectName() {
            // Act
            var actual = strategy.getName();

            // Assert
            var expected = "default";
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnCorrectDescription() {
            // Act
            var actual = strategy.getDescription();

            // Assert
            var expected = "Default aggressive shrink strategy that removes all unused classes";
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class DetermineRequiredClassesTests {

        @Test
        void shouldReturnEmptySetWhenNoEntryPoints() {
            // Arrange
            var analysisResult = createAnalysisResult(
                    Map.of(), // no classes
                    Map.of(), // no dependencies
                    Set.of() // no entry points
                    );

            // Act
            var actual = strategy.determineRequiredClasses(analysisResult);

            // Assert
            assertThat(actual).isEmpty();
        }

        @Test
        void shouldReturnOnlyEntryPointWhenNoDependencies() {
            // Arrange
            var entryPoint = "com.example.Main";
            var analysisResult = createAnalysisResult(
                    Map.of(entryPoint, createClassInfo(entryPoint)),
                    Map.of(entryPoint, Set.of()), // no dependencies
                    Set.of(entryPoint));

            // Act
            var actual = strategy.determineRequiredClasses(analysisResult);

            // Assert
            assertThat(actual).containsExactly(entryPoint);
        }

        @Test
        void shouldIncludeDirectDependencies() {
            // Arrange
            var entryPoint = "com.example.Main";
            var dependency = "com.example.Service";
            var analysisResult = createAnalysisResult(
                    Map.of(
                            entryPoint, createClassInfo(entryPoint),
                            dependency, createClassInfo(dependency)),
                    Map.of(
                            entryPoint, Set.of(dependency),
                            dependency, Set.of()),
                    Set.of(entryPoint));

            // Act
            var actual = strategy.determineRequiredClasses(analysisResult);

            // Assert
            assertThat(actual).containsExactlyInAnyOrder(entryPoint, dependency);
        }

        @Test
        void shouldIncludeTransitiveDependencies() {
            // Arrange
            var entryPoint = "com.example.Main";
            var service = "com.example.Service";
            var repository = "com.example.Repository";
            var analysisResult = createAnalysisResult(
                    Map.of(
                            entryPoint, createClassInfo(entryPoint),
                            service, createClassInfo(service),
                            repository, createClassInfo(repository)),
                    Map.of(
                            entryPoint, Set.of(service),
                            service, Set.of(repository),
                            repository, Set.of()),
                    Set.of(entryPoint));

            // Act
            var actual = strategy.determineRequiredClasses(analysisResult);

            // Assert
            assertThat(actual).containsExactlyInAnyOrder(entryPoint, service, repository);
        }

        @Test
        void shouldHandleMultipleEntryPoints() {
            // Arrange
            var entryPoint1 = "com.example.Main1";
            var entryPoint2 = "com.example.Main2";
            var sharedService = "com.example.SharedService";
            var analysisResult = createAnalysisResult(
                    Map.of(
                            entryPoint1, createClassInfo(entryPoint1),
                            entryPoint2, createClassInfo(entryPoint2),
                            sharedService, createClassInfo(sharedService)),
                    Map.of(
                            entryPoint1, Set.of(sharedService),
                            entryPoint2, Set.of(sharedService),
                            sharedService, Set.of()),
                    Set.of(entryPoint1, entryPoint2));

            // Act
            var actual = strategy.determineRequiredClasses(analysisResult);

            // Assert
            assertThat(actual).containsExactlyInAnyOrder(entryPoint1, entryPoint2, sharedService);
        }

        @Test
        void shouldHandleCircularDependencies() {
            // Arrange
            var classA = "com.example.ClassA";
            var classB = "com.example.ClassB";
            var analysisResult = createAnalysisResult(
                    Map.of(
                            classA, createClassInfo(classA),
                            classB, createClassInfo(classB)),
                    Map.of(
                            classA, Set.of(classB),
                            classB, Set.of(classA)),
                    Set.of(classA));

            // Act
            var actual = strategy.determineRequiredClasses(analysisResult);

            // Assert
            assertThat(actual).containsExactlyInAnyOrder(classA, classB);
        }

        @Test
        void shouldIgnoreUnreachableClasses() {
            // Arrange
            var entryPoint = "com.example.Main";
            var reachableService = "com.example.ReachableService";
            var unreachableService = "com.example.UnreachableService";
            var analysisResult = createAnalysisResult(
                    Map.of(
                            entryPoint, createClassInfo(entryPoint),
                            reachableService, createClassInfo(reachableService),
                            unreachableService, createClassInfo(unreachableService)),
                    Map.of(
                            entryPoint, Set.of(reachableService),
                            reachableService, Set.of(),
                            unreachableService, Set.of() // not reachable from entry point
                            ),
                    Set.of(entryPoint));

            // Act
            var actual = strategy.determineRequiredClasses(analysisResult);

            // Assert
            assertThat(actual).containsExactlyInAnyOrder(entryPoint, reachableService);
            assertThat(actual).doesNotContain(unreachableService);
        }

        @Test
        void shouldHandleComplexDependencyGraph() {
            // Arrange
            var main = "com.example.Main";
            var controller = "com.example.Controller";
            var service1 = "com.example.Service1";
            var service2 = "com.example.Service2";
            var repository = "com.example.Repository";
            var util = "com.example.Util";
            var unused = "com.example.Unused";

            var analysisResult = createAnalysisResult(
                    Map.of(
                            main, createClassInfo(main),
                            controller, createClassInfo(controller),
                            service1, createClassInfo(service1),
                            service2, createClassInfo(service2),
                            repository, createClassInfo(repository),
                            util, createClassInfo(util),
                            unused, createClassInfo(unused)),
                    Map.of(
                            main, Set.of(controller),
                            controller, Set.of(service1, service2),
                            service1, Set.of(repository, util),
                            service2, Set.of(repository),
                            repository, Set.of(util),
                            util, Set.of(),
                            unused, Set.of() // not connected to main
                            ),
                    Set.of(main));

            // Act
            var actual = strategy.determineRequiredClasses(analysisResult);

            // Assert
            assertThat(actual).containsExactlyInAnyOrder(main, controller, service1, service2, repository, util);
            assertThat(actual).doesNotContain(unused);
        }
    }

    // Helper methods
    private AnalysisResult createAnalysisResult(
            Map<String, ClassInfo> allClasses, Map<String, Set<String>> dependencyGraph, Set<String> entryPoints) {
        var statistics = new AnalysisStatistics(
                allClasses.size(),
                entryPoints.size(),
                allClasses.size() - entryPoints.size(),
                Duration.ofMillis(100),
                Map.of());

        return new AnalysisResult(allClasses, dependencyGraph, entryPoints, List.of(), statistics);
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
}
