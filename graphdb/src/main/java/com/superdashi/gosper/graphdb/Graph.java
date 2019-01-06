package com.superdashi.gosper.graphdb;

import com.superdashi.gosper.framework.Namespace;

public final class Graph {

	final View view; // a view that may limit visibility of the graph
	final Visit visit; // holds dirty & canonical part data
	final Restriction restriction;
	final Indices indices; // cached for efficiency
	final Namespace viewer; // cached for efficiency


	Graph(Visit visit, Restriction restriction) {
		assert visit != null;
		assert restriction != null;
		this.view = visit.view;
		this.visit = visit;
		this.restriction = restriction;

		indices = visit.indices;
		viewer = view.namespace;
	}

	public Graph restrictionToNodes(Selector nodes) {
		if (nodes == null) throw new IllegalArgumentException("null nodes");
		if (nodes == Selector.any()) return this;
		Selector edges = Selector.edgesFrom(nodes).and(Selector.edgesTo(nodes));
		if (Space.DUMP_RESTRICTION) {
			System.out.println("-- NODES IN RESTRICTION");
			nodes(nodes).forEach(n -> System.out.println(n));
			System.out.println("-- EDGES IN RESTRICTION");
			edges(edges).forEach(e -> System.out.println(e));
		}
		return new Graph(visit, new Restriction(nodes, edges, visit));
	}

//TODO needs Selectors that can limit nodes to edge targets/sources
//	public Graph restrictionToEdges(Selector edges) {
//		if (edges == null) throw new IllegalArgumentException("null edges");
//		if (edges == Selector.any()) return this;
//		Selector nodes = Selector.
//		return new Graph(visit, new Restriction(nodes, edges, visit));
//	}

	public NodeCursor nodes() {
		Resolver resolver = new Resolver(this);
		return new NodeCursor(indices.allNodes(resolver), resolver);
	}

	public NodeCursor nodes(Selector selector) {
		if (selector == null) throw new IllegalArgumentException("null selector");
		Resolver resolver = new Resolver(this);
		return new NodeCursor(selector.selectNodes(resolver), resolver);
	}

	public EdgeCursor edges() {
		Resolver resolver = new Resolver(this);
		return new EdgeCursor(indices.allEdges(resolver), resolver);
	}

	public EdgeCursor edges(Selector selector) {
		if (selector == null) throw new IllegalArgumentException("null selector");
		Resolver resolver = new Resolver(this);
		return new EdgeCursor(selector.selectEdges(resolver), resolver);
	}

	public OutgoingEdges edgesFrom(Node node) {
		return new OutgoingEdges(this, node);
	}

	public IncomingEdges edgesTo(Node node) {
		return new IncomingEdges(this, node);
	}

}
