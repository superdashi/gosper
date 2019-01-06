package com.superdashi.gosper.item;

public enum Flavor {

	GENERIC,
	NAVIGATION,
	ITEM,
	LIST,
	ERROR,
	MODAL,
	INPUT;

	private static final Flavor[] values = values();

	public static Flavor valueOf(int ordinal) {
		if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid ordinal");
		return values[ordinal];
	}

}
