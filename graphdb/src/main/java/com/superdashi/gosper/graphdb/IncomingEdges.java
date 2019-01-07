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
