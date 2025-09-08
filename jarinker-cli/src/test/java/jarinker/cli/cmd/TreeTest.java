package jarinker.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeTest {

    @Nested
    class OfTests {

        @Test
        void shouldCreateEmptyTreeMapWhenGraphIsEmpty() {
            // Arrange
            var graph = Map.<String, Set<String>>of();

            // Act
            var actual = Tree.of(graph);

            // Assert
            var expected = Map.<String, Tree<String>>of();
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldCreateSingleTreeWhenGraphHasOneNodeWithNoDependencies() {
            // Arrange
            var graph = Map.of("A", Set.<String>of());

            // Act
            var actual = Tree.of(graph);

            // Assert
            var expected = Map.of("A", new Tree<>(new Tree.Node<>("A", 0, Set.of())));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldCreateTreeWithSingleLevelDependencies() {
            // Arrange
            var graph = Map.of(
                    "A", Set.of("B", "C"),
                    "B", Set.<String>of(),
                    "C", Set.<String>of());

            // Act
            var actual = Tree.of(graph);

            // Assert
            var expected = Map.of(
                    "A",
                            new Tree<>(new Tree.Node<>(
                                    "A",
                                    0,
                                    Set.of(new Tree.Node<>("B", 1, Set.of()), new Tree.Node<>("C", 1, Set.of())))),
                    "B", new Tree<>(new Tree.Node<>("B", 0, Set.of())),
                    "C", new Tree<>(new Tree.Node<>("C", 0, Set.of())));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldCreateTreeWithMultipleLevelDependencies() {
            // Arrange
            var graph = Map.of(
                    "A", Set.of("B"),
                    "B", Set.of("C"),
                    "C", Set.<String>of());

            // Act
            var actual = Tree.of(graph);

            // Assert
            var expected = Map.of(
                    "A",
                            new Tree<>(new Tree.Node<>(
                                    "A",
                                    0,
                                    Set.of(new Tree.Node<>("B", 1, Set.of(new Tree.Node<>("C", 2, Set.of())))))),
                    "B", new Tree<>(new Tree.Node<>("B", 0, Set.of(new Tree.Node<>("C", 1, Set.of())))),
                    "C", new Tree<>(new Tree.Node<>("C", 0, Set.of())));
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldHandleNullDependenciesInGraph() {
            // Arrange
            var graph = Map.of(
                    "A", Set.of("B"),
                    "B", Set.<String>of());

            // Act
            var actual = Tree.of(graph);

            // Assert
            var expected = Map.of(
                    "A", new Tree<>(new Tree.Node<>("A", 0, Set.of(new Tree.Node<>("B", 1, Set.of())))),
                    "B", new Tree<>(new Tree.Node<>("B", 0, Set.of())));
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class HasNodeTests {

        @Test
        void shouldReturnTrueWhenNodeExistsAtRoot() {
            // Arrange
            var rootNode = new Tree.Node<>("A", 0, Set.of());
            var tree = new Tree<>(rootNode);

            // Act
            var actual = tree.hasNode("A");

            // Assert
            var expected = true;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnTrueWhenNodeExistsInChildren() {
            // Arrange
            var childNode = new Tree.Node<>("B", 1, Set.of());
            var rootNode = new Tree.Node<>("A", 0, Set.of(childNode));
            var tree = new Tree<>(rootNode);

            // Act
            var actual = tree.hasNode("B");

            // Assert
            var expected = true;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnTrueWhenNodeExistsInDeepChildren() {
            // Arrange
            var deepChildNode = new Tree.Node<>("C", 2, Set.of());
            var childNode = new Tree.Node<>("B", 1, Set.of(deepChildNode));
            var rootNode = new Tree.Node<>("A", 0, Set.of(childNode));
            var tree = new Tree<>(rootNode);

            // Act
            var actual = tree.hasNode("C");

            // Assert
            var expected = true;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnFalseWhenNodeDoesNotExist() {
            // Arrange
            var rootNode = new Tree.Node<>("A", 0, Set.of());
            var tree = new Tree<>(rootNode);

            // Act
            var actual = tree.hasNode("B");

            // Assert
            var expected = false;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class FindNodeTests {

        @Test
        void shouldReturnRootNodeWhenSearchingForRootValue() {
            // Arrange
            var rootNode = new Tree.Node<>("A", 0, Set.of());
            var tree = new Tree<>(rootNode);

            // Act
            var actual = tree.findNode("A");

            // Assert
            var expected = rootNode;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnChildNodeWhenSearchingForChildValue() {
            // Arrange
            var childNode = new Tree.Node<>("B", 1, Set.of());
            var rootNode = new Tree.Node<>("A", 0, Set.of(childNode));
            var tree = new Tree<>(rootNode);

            // Act
            var actual = tree.findNode("B");

            // Assert
            var expected = childNode;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnDeepChildNodeWhenSearchingForDeepChildValue() {
            // Arrange
            var deepChildNode = new Tree.Node<>("C", 2, Set.of());
            var childNode = new Tree.Node<>("B", 1, Set.of(deepChildNode));
            var rootNode = new Tree.Node<>("A", 0, Set.of(childNode));
            var tree = new Tree<>(rootNode);

            // Act
            var actual = tree.findNode("C");

            // Assert
            var expected = deepChildNode;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnNullWhenNodeDoesNotExist() {
            // Arrange
            var rootNode = new Tree.Node<>("A", 0, Set.of());
            var tree = new Tree<>(rootNode);

            // Act
            var actual = tree.findNode("B");

            // Assert
            Tree.Node<String> expected = null;
            assertThat(actual).isEqualTo(expected);
        }
    }
}
