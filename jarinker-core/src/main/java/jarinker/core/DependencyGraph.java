package jarinker.core;

import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.Graph;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    private final Map<String, Set<String>> dependenciesMap;

    public DependencyGraph(Graph<DepsAnalyzer.Node> graph, AnalyzerType analysisType) {
        this.graph = graph;
        this.analysisType = analysisType;
        this.dependenciesMap = buildDependenciesMap(graph);
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
     * Get dependencies grouped by their source for better visualization.
     *
     * @return map of node name to its dependencies
     */
    private static Map<String, Set<String>> buildDependenciesMap(Graph<DepsAnalyzer.Node> graph) {
        var result = new HashMap<String, Set<String>>();
        for (var node : graph.nodes()) {
            var dependencies =
                    graph.adjacentNodes(node).stream().map(Object::toString).collect(Collectors.toSet());
            if (!dependencies.isEmpty()) {
                result.computeIfAbsent(node.toString(), k -> new HashSet<>()).addAll(dependencies);
            }
        }
        return result;
    }
}
