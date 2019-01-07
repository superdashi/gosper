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
package com.superdashi.gosper.studio;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public interface Planar {

	boolean opaque();

	int readPixel(int x, int y);

	default Frame view(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (bounds.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
		IntDimensions dimensions = bounds.dimensions();
		return new Frame() {
			@Override public boolean opaque() { return Planar.this.opaque(); }
			@Override public IntDimensions dimensions() { return dimensions; }
			@Override public int readPixel(int x, int y) {
				if (!dimensions.extendsToPoint(x, y)) throw new IllegalArgumentException("out of bounds");
				return Planar.this.readPixel(bounds.minX + x, bounds.minY + y);
			}
		};
	}

}
