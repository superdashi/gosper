package com.superdashi.gosper.graphdb;

public final class OutgoingEdges extends Edges {

	OutgoingEdges(Graph graph, Node source) {
		super(graph, source);
		checkMatch(source);
	}

	@Override
	public Edge add(Node target) {
		return graph.visit.createEdge(node, target);
	}

	//TODO could make minor optimization and set type directly on ...
	@Override
	public Edge add(Node target, Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		node.checkAvailable(type);
		Edge edge = add(target);
		edge.typeImpl(type);
		return edge;
	}

	@Override
	public Edge add(Node target, String typeName) {
		return add(target, node.newType(typeName));
	}

	@Override
	public int count() {
		return graph.indices.countOfEdgesWithSource(graph, node.id);
	}

	public EdgeCursor withTarget(Node target) {
		checkMatch(target);
		return cursor().intersect(edgesWithTarget(target));
	}

	@Override
	EdgeCursor newCursor() {
		return edgesWithSource(node);
	}
}
