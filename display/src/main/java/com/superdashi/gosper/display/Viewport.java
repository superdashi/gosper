package com.superdashi.gosper.display;

import com.jogamp.opengl.GL;
import com.superdashi.gosper.core.Resolution;
import com.tomgibara.intgeom.IntRect;

public final class Viewport {

	public static Viewport from(IntRect area) {
		if (area == null) throw new IllegalArgumentException("null area");
		return new Viewport(area);
	}

	public final IntRect area;
	public final Resolution resolution;

	private Viewport(IntRect area) {
		this.area = area;
		resolution = Resolution.ofRect(area);
	}

	public void shape(GL gl) {
		gl.glViewport(area.minX, area.minY, area.maxX - area.minX, area.maxY - area.minY);
	}


	// object methods

	@Override
	public int hashCode() {
		return area.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Viewport)) return false;
		Viewport that = (Viewport) obj;
		return this.area.equals(that.area);
	}

	@Override
	public String toString() {
		return area.toString();
	}
}
