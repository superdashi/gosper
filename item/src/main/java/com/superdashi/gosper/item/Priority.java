package com.superdashi.gosper.item;

public enum Priority {
	NONE,
	LOW,
	MEDIUM,
	HIGH,
	URGENT;

	public static final Priority LOWEST = NONE;
	public static final Priority HIGHEST = URGENT;

	private static final Priority[] VALUES = values();

	public static Priority valueOf(int ordinal) {
		try {
			return VALUES[ordinal];
		} catch (ArrayIndexOutOfBoundsException e) {
			if (ordinal < 0) throw new IllegalArgumentException("negative ordinal");
			throw new IllegalArgumentException("invalid ordinal");
		}
	}
}
