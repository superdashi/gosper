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
package com.superdashi.gosper.layout;

public final class Alignment2D {

	private static final Alignment2D[] byOrdinal = new Alignment2D[9];

	public static Alignment2D pair(Alignment horizontal, Alignment vertical) {
		if (horizontal == null) throw new IllegalArgumentException("null horizontal");
		if (vertical == null) throw new IllegalArgumentException("null vertical");
		if (horizontal.ordinal < 0 || vertical.ordinal < 0) return new Alignment2D(horizontal, vertical);
		int ordinal = horizontal.ordinal + 3 * vertical.ordinal;
		Alignment2D alignment = byOrdinal[ordinal];
		if (alignment == null) alignment = byOrdinal[ordinal] = new Alignment2D(horizontal, vertical);
		return alignment;
	}

	public final Alignment horizontal;
	public final Alignment vertical;
	final int ordinal;

	private Alignment2D(Alignment horizontal, Alignment vertical) {
		this.horizontal = horizontal;
		this.vertical = vertical;
		ordinal = -1;
	}

	// object methods

	@Override
	public int hashCode() {
		return ordinal >= 0 ? ordinal : horizontal.hashCode() + 31 * vertical.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Alignment2D)) return false;
		Alignment2D that = (Alignment2D) obj;
		return this.ordinal == that.ordinal || this.horizontal.equals(that.horizontal) && this.vertical.equals(that.vertical);
	}

	@Override
	public String toString() {
		return "horizontal " + horizontal + ", vertical " + vertical;
	}

}
