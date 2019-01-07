/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.superdashi.gosper.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.device.network.WifiAccessPoint;
import com.superdashi.gosper.device.network.WifiEntry;
import com.superdashi.gosper.device.network.WifiProtocol;
import com.superdashi.gosper.device.network.WifiStatus;
import com.superdashi.gosper.device.network.WifiStatus.State;

public class TestWifi implements Wifi {

	private static final WifiAccessPoint[] sampleAPs = {
			WifiAccessPoint.create("BTWifi-X", WifiProtocol.WEP),
			WifiAccessPoint.create("AndroidAP", WifiProtocol.WPA2_PSK),
			WifiAccessPoint.create("Telekom_FON", WifiProtocol.WPA_PSK),
			WifiAccessPoint.create("Vodafone Hotspot", WifiProtocol.WPA_PSK),
			WifiAccessPoint.create("NETGEAR60", WifiProtocol.WPA2_PSK),
			WifiAccessPoint.create("ATT120", WifiProtocol.WEP),
			WifiAccessPoint.create("DG1670AA2", WifiProtocol.WPA2_PSK)
	};

	private final Random random; 
	private String ipAddr = "192.168.1.111";
	private long lastScanTime = -1L;
	private List<WifiAccessPoint> lastScan = null;

	private final Map<String, WifiEntry> entries = new HashMap<>();

	private WifiEntry toBeEnabled = null;
	private long reconfiguredTime = -1L;
	private WifiEntry enabled = null;

	TestWifi(Random random) {
		this.random = random;
	}

	@Override
	public List<WifiAccessPoint> scan(long newerThan) throws IOException {
		if (lastScan != null && newerThan <= lastScanTime) return lastScan;
		List<WifiAccessPoint>  scan = new ArrayList<>();
		if (enabled != null) scan.add(WifiAccessPoint.create(enabled.ssid(), enabled.protocol()));
		for (WifiAccessPoint ap : sampleAPs) {
			if (enabled != null && enabled.ssid().equals(ap.ssid())) continue;
			if (random.nextBoolean()) {
				scan.add(WifiAccessPoint.create(ap.ssid(), ap.protocol(), random.nextBoolean(), random.nextFloat()));
			}
		}
		try {
			Thread.sleep((long) (1000 + 2000 * random.nextFloat()));
		} catch (InterruptedException e) {
			/* ignored */
		}
		lastScan = Collections.unmodifiableList(scan);
		lastScanTime = System.currentTimeMillis();
		return lastScan;
	}

	@Override
	public List<WifiEntry> entries() throws IOException {
		return Collections.unmodifiableList( new ArrayList<>(entries.values()) );
	}

	@Override
	public void updateEntry(WifiEntry entry) throws IOException {
		entries.put(entry.ssid(), entry);
		
	}

	@Override
	public boolean removeEntry(WifiEntry entry) throws IOException {
		return entries.remove(entry.ssid()) != null;
	}

	@Override
	public boolean enableOnly(WifiEntry entry) throws IOException {
		if (entry == null) throw new IllegalArgumentException("null entry");
		toBeEnabled = entry;
		return true;
	}

	@Override
	public boolean reconfigure() throws IOException {
		reconfiguredTime = System.currentTimeMillis();
		enabled = toBeEnabled;
		return true;
	}

	@Override
	public WifiStatus status() throws IOException {
		if (enabled == null) return WifiStatus.disconnected();
		long now = System.currentTimeMillis();
		WifiAccessPoint ap = WifiAccessPoint.create(enabled.ssid(), enabled.protocol(), true, random.nextFloat());
		if (now - reconfiguredTime < 1000) return WifiStatus.create(State.ASSOCIATING, ap, ipAddr);
		return WifiStatus.create(State.COMPLETED, ap, ipAddr);
	}
}
