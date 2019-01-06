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
