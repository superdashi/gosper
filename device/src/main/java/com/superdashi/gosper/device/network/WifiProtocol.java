package com.superdashi.gosper.device.network;

public enum WifiProtocol {

	WEP, WPA_PSK, WPA2_PSK;

	private static final WifiProtocol[] values = values();

	public static WifiProtocol valueOf(int ordinal) {
		if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid ordinal");
		return values[ordinal];
	}

	public boolean secured() {
		return this != WEP;
	}
}
