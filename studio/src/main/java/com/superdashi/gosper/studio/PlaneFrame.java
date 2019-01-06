package com.superdashi.gosper.studio;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

class PlaneFrame<P extends Plane> implements Frame {

	final P plane;
	final IntRect bounds;
	final IntDimensions dimensions;

	PlaneFrame(P plane, IntRect bounds) {
		this.plane = plane;
		this.bounds = bounds;
		this.dimensions = bounds.dimensions();
	}

	@Override
	public boolean opaque() {
		return plane.opaque();
	}

	@Override
	public int readPixel(int x, int y) {
		if (!dimensions.extendsToUnit(x, y)) throw new IllegalArgumentException("out of bounds");
		return plane.readPixel(bounds.minX + x, bounds.minY + y);
	}

	@Override
	public IntDimensions dimensions() {
		return dimensions;
	}

	@Override
	public BufferedImage toImage() {
		int width = dimensions.width;
		int height = dimensions.height;
		BufferedImage image = new BufferedImage(width, height, ImageSurface.imageType(opaque()));
		Graphics2D g = image.createGraphics();
		g.translate(-bounds.minX, -bounds.minY);
		g.setPaint(plane.asShader().toPaint());
		g.fillRect(bounds.minX, bounds.minY, width, height);
		g.dispose();
		return image;
	}

}
