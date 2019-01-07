/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
