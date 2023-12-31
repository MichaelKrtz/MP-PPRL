package protocols;

import db.Record;

import java.util.HashSet;
import java.util.Set;

public class WeightedGraph {
    private final Set<Vertex> vertices;
    private final Set<Edge> edges;

    public WeightedGraph() {
        vertices = new HashSet<>();
        edges = new HashSet<>();
    }

    public void addVertex(Vertex v) {
        vertices.add(v);
    }

    public void addEdge(Vertex v, Record r) {
        edges.add(new Edge(v, r));
    }

    public Set<Vertex> getVertices() {
        return vertices;
    }

    public Set<Edge> getEdges() {
        return edges;
    }
}
