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

final class ImageMask implements Mask {

	// statics

	private static final int MASK_TYPE = BufferedImage.TYPE_BYTE_GRAY;

	static boolean isValidType(int type) {
		return type == MASK_TYPE;
	}

	// fields

	private final IntDimensions dimensions;
	final BufferedImage image;

	// constructors

	//  wrap a buffered image
	ImageMask(BufferedImage image) {
		assert image.getType() == MASK_TYPE;
		this.image = image;
		dimensions = IntDimensions.of(image.getWidth(), image.getHeight());
	}

	ImageMask(IntDimensions dimensions) {
		this.dimensions = dimensions;
		image = new BufferedImage(dimensions.width, dimensions.height, MASK_TYPE);
	}

	// fallback implementation, when nothing is known about frame
	ImageMask(Frame frame) {
		dimensions = frame.dimensions();
		image = new BufferedImage(dimensions.width, dimensions.height, MASK_TYPE);
		int width = dimensions.width;
		int height = dimensions.height;
		int[] input = new int[width];
		byte[] output = new byte[width];
		for (int y = 0; y < height; y++) {
			frame.readScanline(y, input);
			for (int i = 0; i < input.length; i++) {
				output[i] = (byte) (input[i] >> 24);
			}
			writeScanline(y, output);
		}

	}

	// mask methods

	@Override
	public IntDimensions dimensions() {
		return dimensions;
	}

	@Override
	public boolean opaque() {
		return false;
	}

	@Override
	public ImageMask view(IntRect bounds) {
		return new ImageMask( image.getSubimage(bounds.minX, bounds.minY, bounds.width(), bounds.height()) );
	}

	// warning - slow!
	@Override
	public int readPixel(int x, int y) {
		return image.getRGB(x, y);
	}

	@Override
	public BufferedImage toImage() {
		return ImageUtil.copyImage(image);
	}

	@Override
	public Surface materialize() {
		return ImageSurface.over(ImageUtil.convertImage(image, ImageSurface.imageType(true)));
	}

	@Override
	public Mask toMask() {
		return new ImageMask( ImageUtil.copyImage(image) );
	}

	// private helper methods

	private void writeScanline(int y, byte[] scanline) {
		image.getRaster().setDataElements(0, y, scanline.length, 1, scanline);
	}

}
