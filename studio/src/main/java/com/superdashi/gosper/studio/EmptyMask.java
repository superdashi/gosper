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
