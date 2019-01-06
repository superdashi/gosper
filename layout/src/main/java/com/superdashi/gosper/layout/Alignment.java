package com.superdashi.gosper.layout;

public class Alignment {

	public static final Alignment MIN = new Alignment(0.0f, 1.0f, 0);
	public static final Alignment MID = new Alignment(0.5f, 0.5f, 1);
	public static final Alignment MAX = new Alignment(1.0f, 0.0f, 2);

	public static Alignment at(float m) {
		if (m < 0f || m > 1f) throw new IllegalArgumentException("invalid m");
		if (m == 0.0f) return MIN;
		if (m == 0.5f) return MID;
		if (m == 1.0f) return MAX;
		return new Alignment(m);
	}

	public static Alignment atClamped(float m) {
		if (m <= 0.0f) return MIN;
		if (m == 0.5f) return MID;
		if (m >= 1.0f) return MAX;
		return new Alignment(m);
	}

	public final float m;
	public final float i;
	final int ordinal;

	private Alignment(float m) {
		this.m = m;
		this.i = 1f - m;
		ordinal = -1;
	}

	private Alignment(float m, float i, int ordinal) {
		this.m = m;
		this.i = i;
		this.ordinal = ordinal;
	}

	float adjustDelta(float d) {
		return d * m;
	}

	int adjustDelta(int d) {
		return (d * ordinal) >> 1;
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(m) + ordinal;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Alignment)) return false;
		Alignment that = (Alignment) obj;
		return this.m == that.m;
	}

	@Override
	public String toString() {
		switch (ordinal) {
		case 0 : return "minimum";
		case 1 : return "middle";
		case 2 : return "maximum";
		default: return "at " + m;
		}
	}
}