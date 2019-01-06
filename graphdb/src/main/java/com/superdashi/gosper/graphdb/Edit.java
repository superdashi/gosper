package com.superdashi.gosper.graphdb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Edit extends Visit {

	private enum DirtyState {
		CLEAN,
		CHANGES_ONLY,
		ADDITIONS,
		DELETES,
	}

	private final Observer enqueuer;

	private final Set<Part> dirty = new HashSet<>();
	private DirtyState dirtyState = DirtyState.CLEAN; // we don't mix add+mod / deletes in dirty
	private boolean changesMade = false;

	Edit(Space space, View view, Indices indices) {
		super(space, view, indices);
		enqueuer = space.observer; // take a snapshot of this
	}

	//TODO threading???
	public boolean flush() {
		if (dirtyState == DirtyState.CLEAN) {
			assert dirty.isEmpty();
			return false;
		}
		changesMade = true;
		List<Change> changes = new ArrayList<>();
		//TODO should use a while loop?
		for (Part part : new HashSet<>(dirty)) {
			part.cleanData(changes);
			//TODO check valid?
			part.updateIndices(indices);
			enqueuer.observe(part);
		}
		space.flush(changes);
		return true;
	}

	public boolean commit() {
		checkNotClosed();
		flush(); // ensure any changes are flushed
		space.commit(changesMade);
		closeOnly();
		if (!changesMade) return false;
		changesMade = false;
		enqueuer.commit();
		return true;
	}

	public Node createNode() {
		return createNode(view.identityId);
	}

	public Node createNode(Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		checkAvailable(type);
		Node node = createNode();
		node.typeImpl(type);
		return node;
	}

	public Node createNode(String typeName) {
		return createNode(view.type(typeName));
	}

	@Override
	public Edge createEdge(Node source, Node target) {
		if (source == null) throw new IllegalArgumentException("null source");
		if (source.visit != this) throw new IllegalArgumentException("mismatched graph for source");
		if (source.deleted()) throw new IllegalArgumentException("source deleted");
		if (target == null) throw new IllegalArgumentException("null target");
		if (target.visit != this) throw new IllegalArgumentException("mismatched graph for target");
		if (target.deleted()) throw new IllegalArgumentException("target deleted");
		checkMutable();
		Edge edge = new Edge(view.identityId, space.allocateEdgeId(), source, target);
		addEdge(edge);
		return edge;
	}

	public Edge createEdge(Node source, Node target, Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		checkAvailable(type);
		Edge edge = createEdge(source, target);
		edge.typeImpl(type);
		return edge;
	}

	public Edge createEdge(Node source, Node target, String typeName) {
		return createEdge(source, target, view.type(typeName));
	}

	void checkMutable() {
		checkNotClosed();
	}

	@Override
	void recordDirty(Part part) {
		switch (dirtyState) {
		case CLEAN:
			dirtyState = part.deleted() ? DirtyState.DELETES : (part.added() ? DirtyState.ADDITIONS : DirtyState.CHANGES_ONLY);
			break;
		case CHANGES_ONLY:
			if (part.deleted()) {
				dirtyState = DirtyState.DELETES;
			} else if (part.added()) {
				dirtyState = DirtyState.ADDITIONS;
			}
			break;
		case ADDITIONS:
			assert !part.deleted();
			break;
		case DELETES:
			assert part.deleted();
			break;
		}
		dirty.add(part);
	}

	@Override
	void recordClean(Part part) {
		boolean modified = dirty.remove(part);
		assert modified;
		if (dirty.isEmpty()) dirtyState = DirtyState.CLEAN;
	}

	// must flush changes ahead of deletions
	// otherwise node could be created & destroyed in same flush, but we keep only one ref
	@Override
	void flushAdditions() {
		if (dirtyState == DirtyState.ADDITIONS) {
			flush();
		}
	}

	@Override
	void rollback() {
		//TODO pro-actively clear maps?
		enqueuer.rollback();
		space.rollback();
	}

}
