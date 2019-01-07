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

import org.h2.mvstore.MVMap;

import com.superdashi.gosper.item.Value;

// used to map values to parts
final class ValueKey {

	// statics

	//TODO needs sourceId too
	static AttrChange attr(int sourceId, int edgeId, long attrId, Value oldValue, Value newValue) {
		return new AttrChange(
				oldValue == null ? null : new ValueKey(oldValue, sourceId, edgeId),
				newValue == null ? null : new ValueKey(newValue, sourceId, edgeId),
				Name.nsCode(attrId), Name.nmCode(attrId));
	}

	// fields

	final Value value;
	final int sourceId;
	final int edgeId;

	ValueKey(Value value, int sourceId, int edgeId) {
		assert value != null;
		assert sourceId == Integer.MAX_VALUE || Space.isNodeId(sourceId); // max value used to find ceilings
		assert edgeId == Space.NO_EDGE_ID || Space.isEdgeId(edgeId);
		this.value = value;
		this.sourceId = sourceId;
		this.edgeId = edgeId;
	}

	// package scoped methods

	long id() {
		return EdgeKey.id(edgeId, sourceId);
	}

	// object methods

	@Override
	public int hashCode() {
		return value.hashCode() + sourceId + edgeId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ValueKey)) return false;
		ValueKey that = (ValueKey) obj;
		return this.sourceId == that.sourceId && this.edgeId == that.edgeId && this.value.equals(that.value);
	}

	@Override
	public String toString() {
		return "VALUE KEY for " + String.format(edgeId < 0 ? "node %08x" : "edge (%08x,%08x)", sourceId, edgeId) + " of " + value;
	}

	// change classes

	//note cannot combine value and partId into a key, since null value currently indicates remove
	static final class AttrChange implements Change {

		final ValueKey oldValueKey;
		final ValueKey newValueKey;
		final int nsCode; // the namespace of the attr name
		final int nmCode; // the name code of the attr name

		private AttrChange(ValueKey oldValueKey, ValueKey newValueKey, int nsCode, int nmCode) {
			this.oldValueKey = oldValueKey;
			this.newValueKey = newValueKey;
			this.nsCode = nsCode;
			this.nmCode = nmCode;
		}

		@Override
		public void applyTo(Indices indices) {
			long attrId = Name.nsnId(nsCode, nmCode);
			MVMap<ValueKey, Value> index = indices.indicesById.get(attrId);
				if (oldValueKey != null) { // removal
					boolean modified = index.remove(oldValueKey) != null;
					if (!modified) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "no old value key to remove for " + this);
			}
				if (newValueKey != null) { // addition
					boolean modified = index.put(newValueKey, Value.empty()) == null;
					if (!modified) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "existing value key to when adding " + this);
			}
		}

		@Override
		public String toString() {
			String verb;
			if (oldValueKey == null) {
				verb = "ADDED";
			} else if (newValueKey == null) {
				verb = "REMOVED";
			} else {
				verb = "CHANGED";
			}
			ValueKey key = oldValueKey == null ? newValueKey : oldValueKey;
			return verb + String.format(" ATTR (ns %08x) (nm %08x)", nsCode, nmCode) + " ON " + String.format(key.edgeId < 0 ? "node %08x" : "edge (%08x,%08x)", key.sourceId, key.edgeId) + " FROM " + (oldValueKey == null ? " NOTHING" : oldValueKey.value) + (newValueKey == null ? " NOTHING" : newValueKey.value);
		}
	}

}
