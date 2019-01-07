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

import com.superdashi.gosper.item.Value;

final class EdgeKey {

	// combines edge id with source id
	static final long id(int edgeId, int sourceId) {
		return (long) sourceId << 32 | edgeId & 0xffffffffL; // edgeId may be -1 for no edge
	}

	static final int edgeId(long id) {
		return (int) id;
	}

	static final int sourceId(long id) {
		return (int) (id >> 32);
	}

	final int edgeId;
	final int sourceId;
	final int targetId;

	EdgeKey(int edgeId, int sourceId, int targetId) {
		this.edgeId = edgeId;
		this.sourceId = sourceId;
		this.targetId = targetId;
	}

	long id() {
		return id(edgeId, sourceId);
	}

	NodesChange toNodesAddition() {
		return new NodesChange(this, true);
	}

	NodesChange toNodesRemoval() {
		return new NodesChange(this, false);
	}

	// object methods

	@Override
	public int hashCode() {
		return edgeId + sourceId + 31 * targetId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof EdgeKey)) return false;
		EdgeKey that = (EdgeKey) obj;
		return this.edgeId == that.edgeId && this.sourceId == that.sourceId && this.targetId == that.targetId;
	}

	// object methods
	@Override
	public String toString() {
		return String.format("edg: %08x src: %08x trg: %08x", edgeId, sourceId, targetId);
	}

	// change classes

	static final class NodesChange implements Change {

		final EdgeKey key;
		final boolean added;

		private NodesChange(EdgeKey edgeKey, boolean added) {
			this.key = edgeKey;
			this.added = added;
		}

		@Override
		public void applyTo(Indices indices) {
			if (added) {
				boolean modified = indices.edgesByTarget.put(key, Value.empty()) == null;
				if (!modified) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "edge record already existed for " + this);
			} else {
				boolean modified =  indices.edgesByTarget.remove(key) != null;
				if (!modified) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "no edge record to remove for " + this);
			}
		}

		@Override
		public String toString() {
			return (added ? "ADDED " : "REMOVED ") + "TARGET OF " + key;
		}
	}

}
