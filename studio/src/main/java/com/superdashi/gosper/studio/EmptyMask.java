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

import java.awt.image.BufferedImage;
import java.util.Arrays;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

class EmptyMask implements Mask {

	private static final int VALUE = 0xff000000;

	private final IntDimensions dimensions;

	EmptyMask(IntDimensions dimensions) {
		this.dimensions = dimensions;
	}

	@Override
	public IntDimensions dimensions() {
		return dimensions;
	}

	@Override
	public boolean opaque() {
		return false;
	}

	@Override
	public int readPixel(int x, int y) {
		return VALUE;
	}

	@Override
	public EmptyMask view(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (!dimensions.toRect().containsRect(bounds)) throw new IllegalArgumentException("invalid bounds");
		IntDimensions d = bounds.dimensions();
		if (d.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
		return new EmptyMask(d);
	}

	@Override
	public Surface materialize() {
		return ImageSurface.over(toImage());
	}

	@Override
	public BufferedImage toImage() {
		return new BufferedImage(dimensions.width, dimensions.height, ImageSurface.imageType(true));
	}

	@Override
	public Mask toMask() {
		return this;
	}

	@Override
	public void readScanline(int y, int[] scanline) {
		Arrays.fill(scanline, 0, dimensions.width, VALUE);
	}
}
