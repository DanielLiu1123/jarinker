package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JarinkerBuilderTest {

    @Nested
    class CreateTests {

        @Test
        void shouldReturnNewBuilderInstance() {
            // Act
            var actual = JarinkerBuilder.create();

            // Assert
            var expected = JarinkerBuilder.class;
            assertThat(actual).isInstanceOf(expected);
        }
    }

    @Nested
    class WithSourceTests {

        @Test
        void shouldAddSingleSourcePath() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var sourcePath = Paths.get("./src");

            // Act
            var actual = builder.withSource(sourcePath);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldAddMultipleSourcePaths() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var path1 = Paths.get("./src1");
            var path2 = Paths.get("./src2");

            // Act
            var actual = builder.withSource(path1, path2);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldAddSourcePathsFromCollection() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var paths = List.of(Paths.get("./src1"), Paths.get("./src2"));

            // Act
            var actual = builder.withSource(paths);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldIgnoreNullSourcePaths() {
            // Arrange
            var builder = JarinkerBuilder.create();
            Path nullPath = null;

            // Act
            var actual = builder.withSource(nullPath);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class WithDependenciesTests {

        @Test
        void shouldAddSingleDependencyPath() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var dependencyPath = Paths.get("./lib");

            // Act
            var actual = builder.withDependencies(dependencyPath);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldAddMultipleDependencyPaths() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var path1 = Paths.get("./lib1");
            var path2 = Paths.get("./lib2");

            // Act
            var actual = builder.withDependencies(path1, path2);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldAddDependencyPathsFromCollection() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var paths = List.of(Paths.get("./lib1"), Paths.get("./lib2"));

            // Act
            var actual = builder.withDependencies(paths);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class WithOutputDirectoryTests {

        @Test
        void shouldSetOutputDirectoryAndDisableInPlace() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var outputDir = Paths.get("./output");

            // Act
            var actual = builder.withOutputDirectory(outputDir);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class EnableInPlaceOperationTests {

        @Test
        void shouldEnableInPlaceOperation() {
            // Arrange
            var builder = JarinkerBuilder.create();

            // Act
            var actual = builder.enableInPlaceOperation();

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class DisableInPlaceOperationTests {

        @Test
        void shouldDisableInPlaceOperation() {
            // Arrange
            var builder = JarinkerBuilder.create();

            // Act
            var actual = builder.disableInPlaceOperation();

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class WithStrategyTests {

        @Test
        void shouldSetStrategy() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var strategy = ShrinkStrategy.DEFAULT;

            // Act
            var actual = builder.withStrategy(strategy);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class WithConfigurationTests {

        @Test
        void shouldSetConfiguration() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var config = JarinkerConfig.builder().build();

            // Act
            var actual = builder.withConfiguration(config);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class IncludePatternTests {

        @Test
        void shouldAddIncludePattern() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var pattern = "com.example.**";

            // Act
            var actual = builder.includePattern(pattern);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldAddMultipleIncludePatterns() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var patterns = List.of("com.example.**", "org.test.**");

            // Act
            var actual = builder.includePatterns(patterns);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class ExcludePatternTests {

        @Test
        void shouldAddExcludePattern() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var pattern = "**Test";

            // Act
            var actual = builder.excludePattern(pattern);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldAddMultipleExcludePatterns() {
            // Arrange
            var builder = JarinkerBuilder.create();
            var patterns = List.of("**Test", "**Mock");

            // Act
            var actual = builder.excludePatterns(patterns);

            // Assert
            var expected = builder;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class BuildTests {

        @Test
        void shouldThrowExceptionWhenSourcesEmpty() {
            // Arrange
            var builder = JarinkerBuilder.create().withDependencies(Paths.get("./lib"));

            // Act & Assert
            assertThatThrownBy(builder::build)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Source paths cannot be empty");
        }

        @Test
        void shouldThrowExceptionWhenDependenciesEmpty() {
            // Arrange
            var builder = JarinkerBuilder.create().withSource(Paths.get("./src"));

            // Act & Assert
            assertThatThrownBy(builder::build)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Dependency paths cannot be empty");
        }

        @Test
        void shouldBuildJarinkerWithValidConfiguration() {
            // Arrange
            var builder =
                    JarinkerBuilder.create().withSource(Paths.get("./src")).withDependencies(Paths.get("./lib"));

            // Act
            var actual = builder.build();

            // Assert
            var expected = Jarinker.class;
            assertThat(actual).isInstanceOf(expected);
        }
    }
}
