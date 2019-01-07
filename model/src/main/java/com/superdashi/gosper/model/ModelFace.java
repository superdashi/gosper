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

public final class ModelFace {

	public final int index;
	public final Face face;
	public final Daub daub;

	public final Normal n1;
	public final Normal n2;
	public final Normal n3;

	public ModelFace(int index, Face face, Daub daub) {
		this(index, face, daub, null, null, null);
	}

	public ModelFace(int index, Face face, Daub daub, Normal n1, Normal n2, Normal n3) {
		this.index = index;
		this.face = face;
		this.daub =  daub;

		this.n1 = n1 == null ? face.normal : n1;
		this.n2 = n2 == null ? face.normal : n2;
		this.n3 = n3 == null ? face.normal : n3;
	}

	private ModelFace(ModelFace that, Normal n1, Normal n2, Normal n3) {
		this(that.index, that.face, that.daub, n1, n2, n3);
	}

	public ModelFace withDaub(Daub daub) {
		return new ModelFace(index, face, daub, n1, n2, n3);
	}

	public ModelFace withNormals(Normal n1, Normal n2, Normal n3) {
		return new ModelFace(this, n1, n2, n3);
	}

	public ModelFace withN1(Normal n1) {
//		System.out.println("Changing " + this.n1 + " to " + n1);
		return new ModelFace(this, n1, n2, n3);
	}

	public ModelFace withN2(Normal n2) {
//		System.out.println("Changing " + this.n2 + " to " + n2);
		return new ModelFace(this, n1, n2, n3);
	}

	public ModelFace withN3(Normal n3) {
//		System.out.println("Changing " + this.n3 + " to " + n3);
		return new ModelFace(this, n1, n2, n3);
	}

	public ModelFace withNormal(Vertex v, Normal n) {
		Triangle t = face.triangle;
		if (v.equals(t.v1)) return withN1(n);
		if (v.equals(t.v2)) return withN2(n);
		if (v.equals(t.v3)) return withN3(n);
		return this;
	}

}
