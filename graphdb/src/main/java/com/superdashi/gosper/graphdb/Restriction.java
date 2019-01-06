package com.superdashi.gosper.graphdb;

//TODO make flexible
final class Restriction {

	final Selector nodeSel;
	final Selector edgeSel;
	// these fields may be null
	final NodeSequence nodeSeq;
	final EdgeSequence edgeSeq;

	Restriction(Selector sel, Visit visit) {
		this(sel, sel, visit.unrestrictedResolver);
	}

	Restriction(Selector nodeSel, Selector edgeSel, Graph graph) {
		this(nodeSel.and(graph.restriction.nodeSel), edgeSel.and(graph.restriction.edgeSel), graph.visit);
	}

	Restriction(Selector nodeSel, Selector edgeSel, Visit visit) {
		this(nodeSel, edgeSel, visit.unrestrictedResolver);
	}

	Restriction(Selector nodeSel, Selector edgeSel, Resolver resolver) {
		this(
				nodeSel,
				edgeSel,
				nodeSel == Selector.any() ? null : nodeSel.selectNodes(resolver),
				edgeSel == Selector.any() ? null : edgeSel.selectEdges(resolver)
			);
	}

	Restriction(Selector nodeSel, Selector edgeSel, NodeSequence nodesSeq, EdgeSequence edgesSeq) {
		assert nodeSel != null;
		assert edgeSel != null;
		this.nodeSel = nodeSel;
		this.edgeSel = edgeSel;
		this.nodeSeq = nodesSeq;
		this.edgeSeq = edgesSeq;
	}

	boolean unrestricted() {
		return nodeSeq == null && edgeSeq == null;
	}

	boolean nodesRestricted() {
		return nodeSeq != null;
	}

	boolean edgesRestricted() {
		return edgeSeq != null;
	}

}
