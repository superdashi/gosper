package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import com.tomgibara.intgeom.IntDimensions;

final class ClearFrame implements Frame {

	private final IntDimensions dimensions;

	ClearFrame(IntDimensions dimensions) { this.dimensions = dimensions; }

	@Override public boolean opaque() { return false; }
	@Override public int readPixel(int x, int y) { return 0; }
	@Override public IntDimensions dimensions() { return dimensions; }
	@Override public void readScanline(int y, int[] scanline) { Arrays.fill(scanline, 0); }
	@Override public BufferedImage toImage() { return new BufferedImage(dimensions.width, dimensions.height, ImageSurface.imageType(false)); }
	@Override public Surface materialize() { return ImageSurface.over(toImage()); }
	@Override public Mask toMask() { return new EmptyMask(dimensions); }

}
