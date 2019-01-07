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
package com.superdashi.gosper.model;

public final class Triangle {

	public final Vertex v1;
	public final Vertex v2;
	public final Vertex v3;

	public Triangle(Vertex v1, Vertex v2, Vertex v3) {
		if (v1 == null) throw new IllegalArgumentException("null v1");
		if (v2 == null) throw new IllegalArgumentException("null v2");
		if (v3 == null) throw new IllegalArgumentException("null v3");
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}

	@Override
	public String toString() {
		return "[" + v1.toString() + " " + v2.toString() + " " + v3.toString() + "]";
	}
}
