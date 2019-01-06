package com.superdashi.gosper.graphdb;

public final class IncomingEdges extends Edges {

	IncomingEdges(Graph graph, Node target) {
		super(graph, target);
		checkMatch(target);
	}

	@Override
	public Edge add(Node source) {
		return graph.visit.createEdge(source, node);
	}

	//TODO could make minor optimization and set type directly on ...
	@Override
	public Edge add(Node source, Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		node.checkAvailable(type);
		Edge edge = add(source);
		edge.typeImpl(type);
		return edge;
	}

	@Override
	public Edge add(Node source, String typeName) {
		return add(source, node.newType(typeName));
	}

	@Override
	public int count() {
		return graph.indices.countOfEdgesWithTarget(graph, node.id);
	}

	public EdgeCursor withSource(Node source) {
		checkMatch(source);
		return cursor().intersect(edgesWithSource(source));
	}

	@Override
	EdgeCursor newCursor() {
		return edgesWithTarget(node);
	}
}
