package com.superdashi.gosper.item;

import java.util.Optional;

public enum ScreenColor {

	OTHER, // custom or not applicable
	MONO,  // black and white
	GRAY,  // greyscale
	COLOR; // typically, but not necessarily full color

	private static final ScreenColor[] values = values();

	public static ScreenColor valueOf(int ordinal) {
		if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid ordinal");
		return values[ordinal];
	}

	public Optional<ScreenColor> fallback() {
		switch (this) {
		case COLOR: return Optional.of(GRAY);
		case GRAY : return Optional.of(MONO);
		case MONO : return Optional.empty();
		case OTHER: return Optional.of(COLOR);
		default   : return Optional.empty();
		}
	}
}
