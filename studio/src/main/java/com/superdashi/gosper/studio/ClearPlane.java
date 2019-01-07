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

import com.tomgibara.intgeom.IntRect;

public final class ClearPlane implements Plane {

	private static final ClearPlane instance = new ClearPlane();

	public static ClearPlane instance() { return instance; }

	private ClearPlane() { }

	@Override public boolean opaque() { return false; }
	@Override public int readPixel(int x, int y) { return 0; }

	@Override
	public ClearFrame frame(IntRect bounds) {
		return new ClearFrame(bounds.dimensions());
	}

	@Override
	public Shader asShader() {
		return ClearShader.instance();
	}
}
