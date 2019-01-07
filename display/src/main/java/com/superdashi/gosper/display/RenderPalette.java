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
package com.superdashi.gosper.display;

import java.nio.FloatBuffer;

import com.superdashi.gosper.color.Palette;

public final class RenderPalette {

	private final RenderPalettes palettes;
	private final Palette palette;

	int index;
	int refCount = 0;

	private float[] colors;

	RenderPalette(RenderPalettes palettes, Palette palette) {
		this.palettes = palettes;
		this.palette = palette;
	}

	public Palette getPalette() {
		return palette;
	}

	public void destroy() {
		if (palettes == null) throw new IllegalStateException("indestructible");
		palettes.put(this);
	}

	public boolean isValid() {
		return refCount > 0;
	}

	@Override
	public String toString() {
		return "index: " + index + ", palette: " + palette;
	}

	void writeTo(FloatBuffer buffer) {
		if (colors == null) {
			colors = palette.asOpaqueFloats();
		}
		buffer.put(colors);
	}

	RenderPalette indestructible() {
		if (palettes == null) return this;
		RenderPalette copy = new RenderPalette(null, palette);
		copy.index = this.index;
		copy.refCount = 1;
		return copy;
	}

}
