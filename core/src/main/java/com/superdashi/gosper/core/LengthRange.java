package com.superdashi.gosper.core;

import com.superdashi.gosper.config.Config.LengthConfig;

public final class LengthRange {

	public final float min;
	public final float max;
	public final float def;

	public LengthRange(float min, float max, float def) {
		if (min > max) throw new IllegalArgumentException("min exceeds max");
		if (def > max) throw new IllegalArgumentException("def exceeds max");
		if (min > def) throw new IllegalArgumentException("min exceeds def");
		this.min = min;
		this.max = max;
		this.def = def;
	}

	public float lengthOf(LengthConfig style) {
		float length = style.asFloat();
		if (length < min) return min;
		if (length > max) return max;
		return length;
	}

}
