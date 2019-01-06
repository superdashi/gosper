package com.superdashi.gosper.graphdb;

public final class Edge extends Part {

	//TODO should be possible to make these final
	final int sourceId;
	final int targetId;

	private Node source = null;
	private Node target = null;

	Edge(Visit visit, EdgeKey key, PartData data, boolean knownVisible) {
		super(visit, key.edgeId, data, knownVisible);
		sourceId = key.sourceId;
		targetId = key.targetId;
	}

	// used to make new edges
	Edge(long ownerId, int edgeId, Node source, Node target) {
		//TODO currently always called with knownVisible
		super(source.visit, edgeId, new PartData(ownerId), ownerId == source.visit.view.identityId);
		this.sourceId = source.id;
		this.source = source;
		this.targetId = target.id;
		this.target = target;
		recordDirty(PartData.FLAG_DIRTY_OWNER | PartData.FLAG_DIRTY_NODES); // newly created edges are automatically dirty
	}

	public Node source() {
		if (source == null) {
			source = visit.node(sourceId);
		}
		return source.viewable() ? source : null;
	}

	public Node target() {
		if (target == null) {
			target = visit.node(targetId);
		}
		return target.viewable() ? target : null;
	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public boolean isEdge() {
		return true;
	}

	@Override
	public void delete()  {
		checkNotDeleted();
		checkDeletable();
		visit.flushAdditions();
		recordDirty(PartData.FLAG_DELETED);
	}

	@Override
	public String toString() {
		return toString("", new StringBuilder()).toString();
	}

	// package implementations

	@Override int edgeId  () { return       id; }
	@Override int sourceId() { return sourceId; }
	@Override int targetId() { return targetId; }

	@Override EdgeKey edgeKey() { return new EdgeKey(id, sourceId, targetId); }

	@Override
	void updateIndices(Indices indices) {
		if (deleted()) {
			indices.removeEdge(this);
		} else {
			indices.putEdge(this);
		}
	}

	@Override
	StringBuilder toString(String prefix, StringBuilder sb) {
		super.toString(prefix, sb);
		String p = "  " + prefix;
		if (sourceImpl() != null) {
			sb.append(prefix).append("Source:\n");
			sourceImpl().toString(p, sb);
		}
		if (targetImpl() != null) {
			sb.append(prefix).append("Target:\n");
			targetImpl().toString(p, sb);
		}
		return sb;
	}

	// package scoped methods

	Node sourceImpl() {
		return source == null ? source = visit.node(sourceId) : source;
	}

	Node targetImpl() {
		return target == null ? target = visit.node(targetId) : target;
	}
}
