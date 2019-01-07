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
