package com.superdashi.gosper.graphdb;

// used to track edges to a target
final class TargetKeyType extends EdgeKeyType {

	static final TargetKeyType instance = new TargetKeyType();

	@Override
	public int compare(Object a, Object b) {
		EdgeKey ea = (EdgeKey) a;
		EdgeKey eb = (EdgeKey) b;
		int c = Integer.compare(ea.targetId, eb.targetId);
		return c == 0 ? Integer.compare(ea.edgeId, eb.edgeId) : c;
	}

}
