package com.superdashi.gosper.studio;

import com.tomgibara.intgeom.IntRect;

public final class ClearPlane implements Plane {

	private static final ClearPlane instance = new ClearPlane();

	public static ClearPlane instance() { return instance; }

	private ClearPlane() { }

	@Override public boolean opaque() { return false; }
	@Override public int readPixel(int x, int y) { return 0; }

	@Override
	public ClearFrame frame(IntRect bounds) {
		return new ClearFrame(bounds.dimensions());
	}

	@Override
	public Shader asShader() {
		return ClearShader.instance();
	}
}
