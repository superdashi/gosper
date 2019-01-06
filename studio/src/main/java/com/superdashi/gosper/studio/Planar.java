package com.superdashi.gosper.studio;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public interface Planar {

	boolean opaque();

	int readPixel(int x, int y);

	default Frame view(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (bounds.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
		IntDimensions dimensions = bounds.dimensions();
		return new Frame() {
			@Override public boolean opaque() { return Planar.this.opaque(); }
			@Override public IntDimensions dimensions() { return dimensions; }
			@Override public int readPixel(int x, int y) {
				if (!dimensions.extendsToPoint(x, y)) throw new IllegalArgumentException("out of bounds");
				return Planar.this.readPixel(bounds.minX + x, bounds.minY + y);
			}
		};
	}

}
