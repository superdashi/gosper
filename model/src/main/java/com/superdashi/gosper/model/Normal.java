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

public class Normal {

	public final float x;
	public final float y;
	public final float z;

	public Normal(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Normal(float[] xyz) {
		this.x = xyz[0];
		this.y = xyz[1];
		this.z = xyz[2];
	}

	public Vector3 asVector() {
		return new Vector3(x, y, z);
	}

	@Override
	public int hashCode() {
		return asVector().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vertex)) return false;
		return this.asVector().equals(((Vertex) obj).asVector());
	}

	@Override
	public String toString() {
		return asVector().toString();
	}

}
