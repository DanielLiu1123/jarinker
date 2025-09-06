package jarinker.core;

import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.Graph;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple wrapper around jdeps Graph to hide internal implementation.
 *
 * @author Freeman
 */
public class DependencyGraph {

    private final Graph<DepsAnalyzer.Node> graph;

    public DependencyGraph(Graph<DepsAnalyzer.Node> graph) {
        this.graph = graph;
    }

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

    @Override
    public String toString() {
        return "DependencyGraph{nodeCount=" + getNodeCount() + "}";
    }
}
