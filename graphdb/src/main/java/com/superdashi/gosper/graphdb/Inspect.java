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
