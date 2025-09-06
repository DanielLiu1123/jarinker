package jarinker.core;

import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.Graph;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * Simple wrapper around jdeps Graph to hide internal implementation.
 *
 * @author Freeman
 */
@Data
public class DependencyGraph {

    private final Graph<DepsAnalyzer.Node> graph;
    private final AnalyzerType analysisType;

    /**
     * Get the number of nodes in the graph.
     *
     * @return node count
     */
    public int getNodeCount() {
        return graph.nodes().size();
    }

    /**
     * Get all node names.
     *
     * @return set of node names
     */
    public Set<String> getNodeNames() {
        return graph.nodes().stream().map(Object::toString).collect(Collectors.toSet());
    }

    /**
     * Get dependencies for a node.
     *
     * @param nodeName the node name
     * @return set of dependency names
     */
    public Set<String> getDependencies(String nodeName) {
        for (var node : graph.nodes()) {
            if (Objects.equals(node.toString(), nodeName)) {
                return graph.adjacentNodes(node).stream().map(Object::toString).collect(Collectors.toSet());
            }
        }
        return Set.of();
    }

    /**
     * Get dependencies grouped by their source for better visualization.
     *
     * @return map of node name to its dependencies
     */
    public Map<String, Set<String>> getDependenciesMap() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String nodeName : getNodeNames()) {
            Set<String> dependencies = getDependencies(nodeName);
            if (!dependencies.isEmpty()) {
                result.put(nodeName, dependencies);
            }
        }
        return result;
    }

    /**
     * Get nodes that have no dependencies (leaf nodes).
     *
     * @return set of leaf node names
     */
    public Set<String> getLeafNodes() {
        return getNodeNames().stream()
                .filter(nodeName -> getDependencies(nodeName).isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Get nodes that are not depended upon by any other node (root nodes).
     *
     * @return set of root node names
     */
    public Set<String> getRootNodes() {
        Set<String> allNodes = getNodeNames();
        Set<String> dependedUpon =
                getDependenciesMap().values().stream().flatMap(Set::stream).collect(Collectors.toSet());

        return allNodes.stream().filter(node -> !dependedUpon.contains(node)).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "DependencyGraph{nodeCount=" + getNodeCount() + ", type=" + analysisType + "}";
    }
}
