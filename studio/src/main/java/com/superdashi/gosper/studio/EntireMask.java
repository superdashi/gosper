package com.superdashi.gosper.studio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

class EntireMask implements Mask {

	private final int VALUE = 0xffffffff;

	private final IntDimensions dimensions;

	EntireMask(IntDimensions dimensions) {
		this.dimensions = dimensions;
	}

	@Override
	public IntDimensions dimensions() {
		return dimensions;
	}

	@Override
	public boolean opaque() {
		return true;
	}

	@Override
	public int readPixel(int x, int y) {
		return VALUE;
	}

	@Override
	public EntireMask view(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (!dimensions.toRect().containsRect(bounds)) throw new IllegalArgumentException("invalid bounds");
		IntDimensions d = bounds.dimensions();
		if (d.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
		return new EntireMask(d);
	}

	@Override
	public Surface materialize() {
		return ImageSurface.over(toImage());
	}

	@Override
	public BufferedImage toImage() {
		int width = dimensions.width;
		int height = dimensions.height;
		BufferedImage image = new BufferedImage(width, height, ImageSurface.imageType(true));
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.dispose();
		return image;
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
