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

//TODO re-expose edges
public final class Node extends Part {

	Node(Visit visit, int id, PartData data, boolean knownVisible) {
		super(visit, id, data, knownVisible);
	}

	// used to create new, blank nodes
	Node(Visit visit, long ownerId, int id) {
		//TODO currently always called with knownVisible
		super(visit, id, new PartData(ownerId), ownerId == visit.view.identityId);
		//TODO consider making this call in super method?
		recordDirty(PartData.FLAG_DIRTY_OWNER); // newly created nodes are automatically dirty
	}

	@Override
	public boolean isNode() {
		return true;
	}

	@Override
	public boolean isEdge() {
		return false;
	}

	@Override
	public void delete()  {
		checkNotDeleted();
		checkDeletable();
		visit.flushAdditions();
		// check if we can delete all incident edges
		EdgeCursor cursor = visit.incidentEdges(id);
		if (!cursor.stream().allMatch(Edge::deletable)) {
			throw new ConstraintException(ConstraintException.Type.INCIDENT_EDGES);
		}
		cursor.forEach(Edge::delete);
		recordDirty(PartData.FLAG_DELETED);
	}

	// object methods

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Node)) return false;
		Node that = (Node) obj;
		return this.visit == that.visit && this.id == that.id;
	}

	@Override
	public String toString() {
		return toString("", new StringBuilder("Node:\n")).toString();
	}

	// package implementations

	@Override int sourceId() { return id; }
	@Override int edgeId() { return Space.NO_EDGE_ID; }
	@Override int targetId() { return Space.NO_NODE_ID; }

	@Override
	EdgeKey edgeKey() {
		return null;
	}

	@Override
	void updateIndices(Indices indices) {
		if (deleted()) {
			indices.removeNode(this);
		} else {
			indices.putNode(this);
		}
	}
}
