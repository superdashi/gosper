package com.superdashi.gosper.display;

import com.tomgibara.geom.core.Rect;

public class Situation {

	public final RenderPhase phase;
	public final Rect rect;
	public final float z;

	public Situation(RenderPhase phase, Rect rect, float z) {
		if (phase == null) throw new IllegalArgumentException("null phase");
		if (rect == null) throw new IllegalArgumentException("null rect");
		this.phase = phase;
		this.rect = rect;
		this.z = z;
	}
}
