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

public final class PartRef {

	// statics

	public static PartRef fromId(long id) {
		return new PartRef(id);
	}

	// fields

	final long id;

	// constructors

	PartRef(long id) {
		this.id = id;
	}

	// accessors

	public long id() {
		return id;
	}

	// object methods

	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof PartRef)) return false;
		PartRef that = (PartRef) obj;
		return this.id == that.id;
	}

	@Override
	public String toString() {
		return Long.toHexString(id);
	}

	// package scoped methods

	boolean isNode() {
		return EdgeKey.edgeId(id) == Space.NO_EDGE_ID;
	}

	boolean isEdge() {
		return EdgeKey.edgeId(id) != Space.NO_EDGE_ID;
	}

}
