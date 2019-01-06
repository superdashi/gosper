package com.superdashi.gosper.display;

import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;

public class ArtPlane {

	public final float depth;
	// the width that occupies full screen at the plane depth
	public final float width;
	// the height that occupies full screen at the plane depth
	public final float height;
	// the rectangle that occupies full screen at the plane depth
	public final Rect rect;
	// transforms the unit square to the full screen rect at plane depth
	public final Transform trans;

	public ArtPlane(float depth, float hScale, float vScale) {
		this.depth = depth;
		this.width = depth * hScale;
		this.height = depth * vScale;
		rect = Rect.centerAtOrigin(width, height);
		trans = Transform.translateAndScale(Rect.UNIT_SQUARE, rect);
	}

}