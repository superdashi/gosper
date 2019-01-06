package com.superdashi.gosper.graphdb;

public abstract class Edges {

	final Graph graph;
	final Node node;
	private EdgeCursor cursor = null;

	Edges(Graph graph, Node node) {
		assert graph != null;
		assert node != null;
		this.graph = graph;
		this.node = node;
	}

	public EdgeCursor cursor() {
		return cursor == null ? cursor = newCursor() : cursor;
	}

	//TODO better name?
	public EdgeCursor select(Selector selector) {
		if (selector == null) throw new IllegalArgumentException("null selector");
		//TODO this resolver will just be thrown away?
		return cursor().intersect(selector.selectEdges(new Resolver(graph)));
	}

	public boolean empty() {
		return count() == 0;
	}

	public abstract int count();

	public abstract Edge add(Node target);

	public abstract Edge add(Node node, Type type);

	public abstract Edge add(Node node, String typeName);

	abstract EdgeCursor newCursor();

	EdgeCursor edgesWithSource(Node source) {
		Resolver resolver = new Resolver(graph);
		return new EdgeCursor( graph.indices.edgesWithSource(resolver, source.id), resolver );
	}

	EdgeCursor edgesWithTarget(Node target) {
		checkMatch(target);
		Resolver resolver = new Resolver(graph);
		return new EdgeCursor( graph.indices.edgesWithTarget(resolver, target.id), resolver );
	}

	void checkMatch(Node node) {
		if (node == null) throw new IllegalArgumentException("null node");
		if (node.visit != graph.visit) throw new IllegalArgumentException("node not from same visit");
	}
}
