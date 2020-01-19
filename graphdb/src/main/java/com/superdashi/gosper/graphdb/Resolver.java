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

//TODO cache space, because it's heavily used
final class Resolver {

	// fixed fields
	final Visit visit;
	private final Restriction restriction;
	private final boolean nodesRestricted;
	private final boolean edgesRestricted;
	private final boolean knownVisible;

	// node state
	private int lastNodeId = -1;
	private PartData lastNodeData = null;

	// edge state
	private EdgeKey lastEdgeKey = null;
	private long lastEdgeId = -1L;
	private PartData lastEdgeData = null;
	private Edge lastEdge = null;

	// constructors

	Resolver(Graph graph) {
		assert graph != null;
		visit = graph.visit;
		restriction = graph.restriction;
		edgesRestricted = restriction.edgesRestricted();
		nodesRestricted = restriction.nodesRestricted();
		knownVisible = true; // we know this because or retrictions match that of the graph
	}

	Resolver(Visit visit, Selector sel) {
		this(visit, sel, sel);
	}

	private Resolver(Visit visit, Selector nodeSel, Selector edgeSel) {
		assert visit != null;
		this.visit = visit;
		this.restriction = new Restriction(nodeSel, edgeSel, this);
		edgesRestricted = restriction.edgesRestricted();
		nodesRestricted = restriction.nodesRestricted();
		knownVisible = false;
	}

	// part methods

	NodeSequence restrictNodes(NodeSequence seq) {
		return nodesRestricted ? seq.and(restriction.nodeSeq) : seq;
	}

	EdgeSequence restrictEdges(EdgeSequence seq) {
		return edgesRestricted ? seq.and(restriction.edgeSeq) : seq;
	}

	//TODO consider separate methods for node types and edge types
	//TODO currently no efficient way to constrain types
	TypeSequence restrictTypes(TypeSequence seq) {
		if (!edgesRestricted && !nodesRestricted) return seq;
		return () -> seq.stream().filter(tid ->
			restrictNodes(() -> visit.indices.nodeIndices.nodeIteratorOverType(tid)).iterator().hasNext() ||
			restrictEdges(() -> visit.indices.edgeIndices.edgeIteratorOverType(tid)).iterator().hasNext()
		).iterator();
	}

	// no point observing here, node will not normally be request twice
	Node resolveNode(int nodeId) {
		return lastNodeId == nodeId ? visit.node(lastNodeId, lastNodeData, knownVisible) : visit.node(nodeId);
	}

	Edge resolveEdge(long edgeId) {
		if (lastEdgeId == edgeId) {
			return lastEdge == null ? lastEdge = visit.edge(lastEdgeKey, lastEdgeData, knownVisible) : lastEdge;
		} else {
			return visit.edge(EdgeKey.edgeId(edgeId), EdgeKey.sourceId(edgeId), knownVisible);
		}
	}

	Type resolveType(long typeId) {
		return visit.typeForId(typeId);
	}

	void observeNode(int nodeId, PartData nodeData) {
		this.lastNodeId = nodeId;
		this.lastNodeData = nodeData;
	}

	void observeEdge(EdgeKey key, PartData data) {
		lastEdgeKey = key;
		lastEdgeId = key.id();
		lastEdgeData = data;
		lastEdge = null;
	}

	// generic methods

	boolean isFromSameVisitAs(Resolver that) {
		return this.visit == that.visit;
	}


}
