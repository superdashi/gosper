package com.superdashi.gosper.item;

public enum ScreenClass {

	NONE,  // no screen available
	MICRO, // tiny LCD screen, typically black & white
	MINI,  // small for factor approx. vga resolutions
	PC;    // hires full color, typical of PCs

	private static final ScreenClass[] values = values();

	public static ScreenClass valueOf(int ordinal) {
		if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid ordinal");
		return values[ordinal];
	}
}
