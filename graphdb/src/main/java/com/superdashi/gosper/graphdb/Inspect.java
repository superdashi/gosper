package com.superdashi.gosper.graphdb;

public final class Inspect extends Visit {

	Inspect(Space space, View view, Indices indices) {
		super(space, view, indices);
	}

	@Override
	void checkMutable() {
		throw new ConstraintException(ConstraintException.Type.VISIT_STATE, "visit immutable");
	}

	@Override
	Edge createEdge(Node source, Node target) {
		checkMutable();
		return null;
	}

	@Override
	boolean flush() { /* noop */ return false; }

	@Override void recordDirty(Part part) { /* noop */ }
	@Override void recordClean(Part part) { /* noop */ }
	@Override void flushAdditions()       { /* noop */ }
	@Override void rollback()             { /* noop */ }

}
