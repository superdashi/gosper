package com.superdashi.gosper.device.network;

import java.io.IOException;
import java.util.List;

public interface Wifi {

	List<WifiAccessPoint> scan(long newerThan) throws IOException;

	List<WifiEntry> entries() throws IOException;

	void updateEntry(WifiEntry entry) throws IOException;

	boolean removeEntry(WifiEntry entry) throws IOException;

	boolean enableOnly(WifiEntry entry) throws IOException;

	// returns true if reconfiguration succeeded, false if not, IOE if uncertain
	boolean reconfigure() throws IOException;

	WifiStatus status() throws IOException;
}
