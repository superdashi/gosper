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

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public interface Frame extends Planar {

	// attributes

	IntDimensions dimensions();

	// image data

	default void readScanline(int y, int[] scanline) {
		if (scanline == null) throw new IllegalArgumentException("null scanline");
		if (scanline.length < dimensions().width) throw new IllegalArgumentException("scanline too short");
		if (y < 0) throw new IllegalArgumentException("negative y");
		if (y >= dimensions().height) throw new IllegalArgumentException("y exceeds height");
		for (int x = 0; x < scanline.length; x++) {
			scanline[x] = readPixel(x, y);
		}
	}

	// views and copies

	@Override
	default Frame view(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (!dimensions().toRect().containsRect(bounds)) throw new IllegalArgumentException("bounds exceeded");
		return Planar.super.view(bounds);
	}

	default Surface materialize() {
		return new ImageSurface(this);
	}

	default BufferedImage toImage() {
		return ImageSurface.frameToImage(this);
	}

	default Mask toMask() {
		return new ImageMask(this);
	}

}
