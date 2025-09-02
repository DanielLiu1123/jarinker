package jarinker.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JarinkerConfigTest {

    @Nested
    class BuilderTests {

        @Test
        void shouldCreateBuilderWithDefaults() {
            // Act
            var config = JarinkerConfig.builder().build();

            // Assert
            assertThat(config.getStrategy()).isEqualTo(ShrinkStrategy.DEFAULT);
            assertThat(config.getIncludePatterns()).isEmpty();
            assertThat(config.getExcludePatterns()).isEmpty();
            assertThat(config.getOutputDirectory()).isEmpty();
            assertThat(config.isInPlaceOperation()).isTrue();
            assertThat(config.isVerbose()).isFalse();
            assertThat(config.isShowProgress()).isTrue();
            assertThat(config.getTimeout()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        void shouldSetStrategy() {
            // Arrange
            var strategy = ShrinkStrategy.DEFAULT;

            // Act
            var config = JarinkerConfig.builder().strategy(strategy).build();

            // Assert
            assertThat(config.getStrategy()).isEqualTo(strategy);
        }

        @Test
        void shouldAddIncludePattern() {
            // Arrange
            var pattern = "com.example.**";

            // Act
            var config = JarinkerConfig.builder().includePattern(pattern).build();

            // Assert
            assertThat(config.getIncludePatterns()).containsExactly(pattern);
        }

        @Test
        void shouldAddMultipleIncludePatterns() {
            // Arrange
            var patterns = List.of("com.example.**", "org.test.**");

            // Act
            var config = JarinkerConfig.builder().includePatterns(patterns).build();

            // Assert
            assertThat(config.getIncludePatterns()).containsExactlyInAnyOrderElementsOf(patterns);
        }

        @Test
        void shouldAddExcludePattern() {
            // Arrange
            var pattern = "**Test";

            // Act
            var config = JarinkerConfig.builder().excludePattern(pattern).build();

            // Assert
            assertThat(config.getExcludePatterns()).containsExactly(pattern);
        }

        @Test
        void shouldAddMultipleExcludePatterns() {
            // Arrange
            var patterns = List.of("**Test", "**Mock");

            // Act
            var config = JarinkerConfig.builder().excludePatterns(patterns).build();

            // Assert
            assertThat(config.getExcludePatterns()).containsExactlyInAnyOrderElementsOf(patterns);
        }

        @Test
        void shouldSetOutputDirectory() {
            // Arrange
            var outputDir = Paths.get("./output");

            // Act
            var config = JarinkerConfig.builder().outputDirectory(outputDir).build();

            // Assert
            assertThat(config.getOutputDirectory()).isEqualTo(Optional.of(outputDir));
        }

        @Test
        void shouldHandleNullOutputDirectory() {
            // Act
            var config = JarinkerConfig.builder().outputDirectory(null).build();

            // Assert
            assertThat(config.getOutputDirectory()).isEmpty();
        }

        @Test
        void shouldEnableInPlaceOperation() {
            // Act
            var config = JarinkerConfig.builder().enableInPlaceOperation().build();

            // Assert
            assertThat(config.isInPlaceOperation()).isTrue();
            assertThat(config.getOutputDirectory()).isEmpty();
        }

        @Test
        void shouldDisableInPlaceOperation() {
            // Act
            var config = JarinkerConfig.builder().disableInPlaceOperation().build();

            // Assert
            assertThat(config.isInPlaceOperation()).isFalse();
        }

        @Test
        void shouldSetInPlaceOperationDirectly() {
            // Act
            var config1 = JarinkerConfig.builder().inPlaceOperation(true).build();
            var config2 = JarinkerConfig.builder().inPlaceOperation(false).build();

            // Assert
            assertThat(config1.isInPlaceOperation()).isTrue();
            assertThat(config2.isInPlaceOperation()).isFalse();
        }

        @Test
        void shouldSetVerbose() {
            // Act
            var config1 = JarinkerConfig.builder().verbose(true).build();
            var config2 = JarinkerConfig.builder().verbose(false).build();

            // Assert
            assertThat(config1.isVerbose()).isTrue();
            assertThat(config2.isVerbose()).isFalse();
        }

        @Test
        void shouldSetShowProgress() {
            // Act
            var config1 = JarinkerConfig.builder().showProgress(true).build();
            var config2 = JarinkerConfig.builder().showProgress(false).build();

            // Assert
            assertThat(config1.isShowProgress()).isTrue();
            assertThat(config2.isShowProgress()).isFalse();
        }

        @Test
        void shouldSetTimeout() {
            // Arrange
            var timeout = Duration.ofMinutes(5);

            // Act
            var config = JarinkerConfig.builder().timeout(timeout).build();

            // Assert
            assertThat(config.getTimeout()).isEqualTo(timeout);
        }
    }

    @Nested
    class GetterTests {

        @Test
        void shouldReturnImmutableIncludePatterns() {
            // Arrange
            var config =
                    JarinkerConfig.builder().includePattern("com.example.**").build();

            // Act
            var patterns = config.getIncludePatterns();

            // Assert
            assertThat(patterns).isNotSameAs(config.getIncludePatterns());
            assertThat(patterns).containsExactly("com.example.**");
        }

        @Test
        void shouldReturnImmutableExcludePatterns() {
            // Arrange
            var config = JarinkerConfig.builder().excludePattern("**Test").build();

            // Act
            var patterns = config.getExcludePatterns();

            // Assert
            assertThat(patterns).isNotSameAs(config.getExcludePatterns());
            assertThat(patterns).containsExactly("**Test");
        }
    }

    @Nested
    class BuilderFluentInterfaceTests {

        @Test
        void shouldSupportMethodChaining() {
            // Arrange
            var outputDir = Paths.get("./output");
            var timeout = Duration.ofMinutes(5);

            // Act
            var config = JarinkerConfig.builder()
                    .strategy(ShrinkStrategy.DEFAULT)
                    .includePattern("com.example.**")
                    .excludePattern("**Test")
                    .outputDirectory(outputDir)
                    .disableInPlaceOperation()
                    .verbose(true)
                    .showProgress(false)
                    .timeout(timeout)
                    .build();

            // Assert
            assertThat(config.getStrategy()).isEqualTo(ShrinkStrategy.DEFAULT);
            assertThat(config.getIncludePatterns()).containsExactly("com.example.**");
            assertThat(config.getExcludePatterns()).containsExactly("**Test");
            assertThat(config.getOutputDirectory()).isEqualTo(Optional.of(outputDir));
            assertThat(config.isInPlaceOperation()).isFalse();
            assertThat(config.isVerbose()).isTrue();
            assertThat(config.isShowProgress()).isFalse();
            assertThat(config.getTimeout()).isEqualTo(timeout);
        }
    }
}
