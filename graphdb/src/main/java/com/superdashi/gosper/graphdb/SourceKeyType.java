package com.superdashi.gosper.graphdb;

// used to track edges from a source
final class SourceKeyType extends EdgeKeyType {

	static final SourceKeyType instance = new SourceKeyType();

	@Override
	public int compare(Object a, Object b) {
		EdgeKey ea = (EdgeKey) a;
		EdgeKey eb = (EdgeKey) b;
		int c = Integer.compare(ea.sourceId, eb.sourceId);
		return c == 0 ? Integer.compare(ea.edgeId, eb.edgeId) : c;
	}

}
