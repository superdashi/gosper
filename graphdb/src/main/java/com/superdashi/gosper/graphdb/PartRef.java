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
