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

public interface Mask extends Frame {

	//TODO introduce a Masks class?

	public static Mask overImage(BufferedImage image) {
		if (image == null) throw new IllegalArgumentException("null image");
		if (!ImageMask.isValidType(image.getType())) throw new IllegalArgumentException("invalid image type");
		return new ImageMask(image);
	}

	public static Mask overByteGray(IntDimensions dimensions, byte[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 8);
		return new ImageMask(ImageUtil.imageOverByteGray(dimensions, data));
	}

	public static Mask create(IntDimensions dimensions) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		return new ImageMask(dimensions);
	}

	public static Mask empty(IntDimensions dimensions) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		return new EmptyMask(dimensions);
	}

	public static Mask entire(IntDimensions dimensions) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		return new EntireMask(dimensions);
	}

	Mask view(IntRect bounds);

}
