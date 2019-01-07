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

import java.nio.FloatBuffer;

import com.tomgibara.geom.core.Point;

public final class Vector3 {

	public static final Vector3 ZERO = new Vector3(0f, 0f, 0f);

	public final float x;
	public final float y;
	public final float z;

	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3(float[] xyz) {
		this.x = xyz[0];
		this.y = xyz[1];
		this.z = xyz[2];
	}

	public Vertex toVertex() {
		return new Vertex(x, y, z);
	}

	public float dot(Vector3 that) {
		return this.x * that.x + this.y * that.y + this.z * that.z;
	}

	public Point projectionX() {
		return new Point(y, z);
	}

	public Point projectionY() {
		return new Point(x, z);
	}

	public Point projectionZ() {
		return new Point(x, y);
	}

	public float[] toArray() {
		return new float[] {x, y, z};
	}

	public void writeToArray(float[] fs, int i) {
		fs[i    ] = x;
		fs[i + 1] = y;
		fs[i + 2] = z;
	}

	public void writeToBuffer(FloatBuffer buffer) {
		buffer.put(x).put(y).put(z);
	}


	public VectorMath math() {
		return new VectorMath(x,y,z);
	}

	@Override
	public int hashCode() {
		long a = Float.floatToIntBits(x) & 0xffffffffL;
		long b = Float.floatToIntBits(y) & 0xffffffffL;
		long c = Float.floatToIntBits(z) & 0xffffffffL;
		return Long.hashCode(a | b << 21 | c << 42);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Vector3)) return false;
		Vector3 that = (Vector3) obj;
		return this.x == that.x && this.y == that.y && this.z == that.z;
	}

	@Override
	public String toString() {
		return String.format("%10.5f,%10.5f,%10.5f", x, y, z);
	}

}
