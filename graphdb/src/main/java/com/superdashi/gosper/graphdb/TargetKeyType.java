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
