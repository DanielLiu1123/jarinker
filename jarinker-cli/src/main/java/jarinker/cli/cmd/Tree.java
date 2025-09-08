package jarinker.cli.cmd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

record Tree<T>(Node<T> root) {

    public record Node<T>(T value, int depth, Set<Node<T>> children) {}

    public static <T> Map<T, Tree<T>> of(Map<T, Set<T>> graph) {
        var result = new HashMap<T, Tree<T>>();
        for (var en : graph.entrySet()) {
            result.put(en.getKey(), new Tree<>(new Node<>(en.getKey(), 0, buildChildren(en.getValue(), graph, 1))));
        }
        return result;
    }

    private static <T> Set<Node<T>> buildChildren(@Nullable Set<T> deps, Map<T, Set<T>> graph, int depth) {
        if (deps == null || deps.isEmpty()) {
            return Set.of();
        }
        Set<Node<T>> result = new HashSet<>();
        for (T dep : deps) {
            Node<T> node = new Node<>(dep, depth, buildChildren(graph.get(dep), graph, depth + 1));
            result.add(node);
        }
        return result;
    }

    public boolean hasNode(T value) {
        return findNode(value) != null;
    }

    @Nullable
    public Node<T> findNode(T value) {
        return findNode(root, value);
    }

    @Nullable
    private Node<T> findNode(Node<T> node, T value) {
        if (node.value().equals(value)) {
            return node;
        }
        for (var child : node.children()) {
            var found = findNode(child, value);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
