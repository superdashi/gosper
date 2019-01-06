package com.superdashi.gosper.graphdb;

// indicates that the part has the nsn
// name code can be missing (if only mapped by ns)
// edge id can be missing (if mapped to node)
final class NSNKey {

	final int nsCode;
	final int nmCode;
	final int sourceId;
	final int edgeId;

	NSNKey(int nsCode, int nmCode, int sourceId, int edgeId) {
		this.nsCode = nsCode;
		this.nmCode = nmCode;
		this.sourceId = sourceId;
		this.edgeId = edgeId;

		assert Space.isNsCode(nsCode);
		//TODO
		// assert Space.isNmCode(nmCode); not necessarily a nm, could be a type
		assert Space.isNodeId(sourceId);
		assert edgeId == Space.NO_EDGE_ID || Space.isEdgeId(edgeId);
	}

	NSNKey(int nsCode, int nmCode, int sourceId) {
		this(nsCode, nmCode, sourceId, Space.NO_EDGE_ID);
	}

	NSNKey(long nsnId, int sourceId) {
		this( Name.nsCode(nsnId), Name.nmCode(nsnId), sourceId );
	}

	NSNKey(long nsnId, int sourceId, int edgeId) {
		this( Name.nsCode(nsnId), Name.nmCode(nsnId), sourceId, edgeId );
	}

	long nsnId() {
		return Name.nsnId(nsCode, nmCode);
	}

	int nodeId() {
		return sourceId;
	}

	long edgeId() {
		return EdgeKey.id(edgeId, sourceId);
	}

	boolean isEdge() {
		return edgeId != Space.NO_EDGE_ID;
	}

	OwnerChange toOwnerAddition() {
		return new OwnerChange(this, true);
	}

	TypeChange toTypeAddition() {
		return new TypeChange(this, true);
	}

	PermChange toPermAddition() {
		return new PermChange(this, true);
	}

	TagChange toTagAddition() {
		return new TagChange(this, true);
	}

	OwnerChange toOwnerRemoval() {
		return new OwnerChange(this, false);
	}

	TypeChange toTypeRemoval() {
		return new TypeChange(this, false);
	}

	PermChange toPermRemoval() {
		return new PermChange(this, false);
	}

	TagChange toTagRemoval() {
		return new TagChange(this, false);
	}

	// object methods

	@Override
	public int hashCode() {
		return nsCode + nmCode + sourceId + edgeId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof NSNKey)) return false;
		NSNKey that = (NSNKey) obj;
		return
				this.nsCode == that.nsCode &&
				this.nmCode == that.nmCode &&
				this.sourceId == that.sourceId &&
				this.edgeId == that.edgeId  ;
	}

	@Override
	public String toString() {
		return String.format("ns: %08x nm: %08x node: %08x edge: %08x", nsCode, nmCode, sourceId, edgeId);
	}

	// change classes

	static abstract class NSNChange implements Change {

		final NSNKey nsnKey;
		final boolean added;

		NSNChange(NSNKey nsnKey, boolean added) {
			this.nsnKey = nsnKey;
			this.added = added;
		}

		PartIndices chooseIndices(Indices indices) {
			return nsnKey.isEdge() ? indices.edgeIndices : indices.nodeIndices;
		}
	}

	static class OwnerChange extends NSNChange {

		private OwnerChange(NSNKey ownerKey, boolean added) {
			super(ownerKey, added);
		}

		@Override
		public void applyTo(Indices indices) {
			chooseIndices(indices).applyOwnerChange(this);
		}

		@Override
		public String toString() {
			return (added ? "ADDED " : "REMOVED ") + String.format("OWNER %08x ", nsnKey.nsCode) + (added ? "TO " : "FROM ") + String.format(nsnKey.isEdge() ? "EDGE %08x ON %08x " : "NODE %08x ", nsnKey.sourceId, nsnKey.edgeId);
		}

	}

	static final class PermChange extends NSNChange {

		private PermChange(NSNKey typeKey, boolean added) {
			super(typeKey, added);
		}

		@Override
		public void applyTo(Indices indices) {
			chooseIndices(indices).applyPermChange(this);
		}

		@Override
		public String toString() {
			return (added ? "ADDED " : "REMOVED ") + "PERM KEY " + nsnKey;
		}
	}

	static final class TypeChange extends NSNChange {

		private TypeChange(NSNKey typeKey, boolean added) {
			super(typeKey, added);
		}

		@Override
		public void applyTo(Indices indices) {
			chooseIndices(indices).applyTypeChange(this);
		}

		@Override
		public String toString() {
			return (added ? "ADDED " : "REMOVED ") + "TYPE KEY " + nsnKey;
		}
	}

	static final class TagChange extends NSNChange {

		private TagChange(NSNKey tagKey, boolean added) {
			super(tagKey, added);
		}

		@Override
		public void applyTo(Indices indices) {
			chooseIndices(indices).applyTagChange(this);
		}

		@Override
		public String toString() {
			return (added ? "ADDED " : "REMOVED ") + "TAG KEY " + nsnKey;
		}
	}

}
