package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;

import com.tomgibara.intgeom.IntDimensions;

public abstract class Target {

	public static Target toIntRGB(IntDimensions dimensions, int[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 32);
		return new SurfaceTarget(ImageUtil.imageOverIntRGB(dimensions, data));
	}

	public static Target toIntARGB(IntDimensions dimensions, int[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 32);
		return new SurfaceTarget(ImageUtil.imageOverIntARGB(dimensions, data));
	}

	public static Target toShort565ARGB(IntDimensions dimensions, short[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 16);
		return new SurfaceTarget(ImageUtil.imageOverShort565RGB(dimensions, data));
	}

	public static Target toByteBitmap(IntDimensions dimensions, byte[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 32);
		return new SurfaceTarget(ImageUtil.imageOverByteBitmap(dimensions, data));
	}

	public static Target toImage(BufferedImage image) {
		if (image == null) throw new IllegalArgumentException("null image");
		return new SurfaceTarget(image);
	}

	public static Target toSurface(Surface surface) {
		if (surface == null) throw new IllegalArgumentException("null surface");
		if (!surface.isMutable()) throw new IllegalArgumentException("immutable surface");
		return new SurfaceTarget(surface);
	}

	Target() { }

	static class SurfaceTarget extends Target {

		final Surface surface;

		SurfaceTarget(BufferedImage image) {
			surface = ImageSurface.overTarget(image);
		}

		SurfaceTarget(Surface surface) {
			this.surface = surface;
		}

	}
}
