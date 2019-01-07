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
import java.io.IOException;

import javax.imageio.ImageIO;

import com.tomgibara.fundament.Mutability;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.streams.ReadStream;

public interface Surface extends Frame, Mutability<Surface> {

	//TODO move methods to new Surfaces class?

	public static Surface overIntRGB(IntDimensions dimensions, int[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 32);
		return ImageSurface.over(ImageUtil.imageOverIntRGB(dimensions, data));
	}

	public static Surface overIntARGB(IntDimensions dimensions, int[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 32);
		return ImageSurface.over(ImageUtil.imageOverIntARGB(dimensions, data));
	}

	public static Surface overImage(BufferedImage image) {
		return ImageSurface.over(image);
	}

	public static Surface create(IntDimensions dimensions, boolean opaque) {
		return ImageSurface.sized(dimensions, opaque);
	}

	public static Surface decode(ReadStream stream) throws IOException {
		if (stream == null) throw new IllegalArgumentException("null stream");
		BufferedImage image = ImageIO.read(stream.asInputStream());
		int type = image.getType();
		boolean opaque = !image.getColorModel().hasAlpha();
		int reqType = ImageSurface.imageType(opaque);
		if (type != reqType) image = ImageUtil.convertImage(image, reqType);
		return ImageSurface.over(image);
	}

	void writePixel(int x, int y, int argb);

	Surface view(IntRect bounds);

	Canvas createCanvas();

}
