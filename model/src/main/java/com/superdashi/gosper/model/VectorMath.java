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

public final class VectorMath {

	private float x;
	private float y;
	private float z;

	public VectorMath() {
	}

	public VectorMath(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public VectorMath add(Vector3 v) {
		x += v.x;
		y += v.y;
		z += v.z;
		return this;
	}

	public Vector3 toVector3() {
		return new Vector3(x, y, z);
	}

	public Normal toNormal() {
		float s = (float) Math.sqrt(x*x + y*y + z*z);
		return new Normal(x/s, y/s, z/s);
	}

	public Vertex toVertex() {
		return new Vertex(x, y, z);
	}
}
