package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public final class ColorPlane implements Plane {

	private final int argb;

	public ColorPlane(int argb) {
		this.argb = argb;
	}

	@Override
	public boolean opaque() {
		return (argb & 0xff000000) == 0xff000000;
	}

	@Override
	public int readPixel(int x, int y) {
		return argb;
	}

	@Override
	public Frame frame(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (bounds.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
		return new ColorFrame(this, bounds.dimensions());
	}

	@Override
	public Shader asShader() {
		return new ColorShader(argb);
	}

	static final class ColorFrame implements Frame {

		final ColorPlane plane;
		final IntDimensions dimensions;

		ColorFrame(ColorPlane plane, IntDimensions dimensions) {
			this.plane = plane;
			this.dimensions = dimensions;
		}

		@Override
		public boolean opaque() {
			//TODO make coloring helpers visible?
			return (plane.argb & 0xff000000) == 0xff000000;
		}

		@Override
		public int readPixel(int x, int y) {
			return plane.argb;
		}

		@Override
		public IntDimensions dimensions() {
			return dimensions;
		}

		@Override
		public void readScanline(int y, int[] scanline) {
			if (scanline == null) throw new IllegalArgumentException("null scanline");
			if (scanline.length != dimensions.width) throw new IllegalArgumentException("mismatched scanline");
			Arrays.fill(scanline, plane.argb);
		}

		@Override
		public Frame view(IntRect bounds) {
			if (bounds == null) throw new IllegalArgumentException("null bounds");
			if (bounds.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
			if (!dimensions.toRect().containsRect(bounds)) throw new IllegalArgumentException("invalid bounds");
			IntDimensions viewDim = bounds.dimensions();
			return dimensions.equals(viewDim) ? this : new ColorFrame(plane, viewDim);
		}

		@Override
		public BufferedImage toImage() {
			int width = dimensions.width;
			int height = dimensions.height;
			BufferedImage image = new BufferedImage(width, height, ImageSurface.imageType(opaque()));
			WritableRaster raster = image.getRaster();
			int[] scanline = new int[width];
			Arrays.fill(scanline, plane.argb);
			for (int y = 0; y < height; y++) {
				raster.setDataElements(0, y, width, 1, scanline);
			}
			return image;
		}
	}
}
