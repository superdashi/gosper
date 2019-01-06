package com.superdashi.gosper.micro;

import java.io.IOException;
import java.util.List;

import com.superdashi.gosper.bundle.Privileges;
import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.device.network.WifiAccessPoint;
import com.superdashi.gosper.device.network.WifiEntry;
import com.superdashi.gosper.device.network.WifiStatus;

final class PrivilegedWifi implements Wifi {

	private final Wifi wifi;
	private final Privileges privileges;

	PrivilegedWifi(Wifi wifi, Privileges privileges) {
		this.wifi = wifi;
		this.privileges = privileges;
	}

	@Override
	public List<WifiAccessPoint> scan(long newerThan) throws IOException {
		privileges.check(Privileges.SCAN_WIFI_CONNECTIONS);
		return wifi.scan(newerThan);
	}

	@Override
	public List<WifiEntry> entries() throws IOException {
		privileges.check(Privileges.KNOW_WIFI_CONNECTIONS);
		return wifi.entries();
	}

	@Override
	public boolean enableOnly(WifiEntry entry) throws IOException {
		privileges.check(Privileges.CHOOSE_WIFI_CONNECTION);
		return wifi.enableOnly(entry);
	}

	@Override
	public void updateEntry(WifiEntry entry) throws IOException {
		privileges.check(Privileges.ADD_WIFI_CONNECTION);
		wifi.updateEntry(entry);
	}

	@Override
	public boolean removeEntry(WifiEntry entry) throws IOException {
		privileges.check(Privileges.REMOVE_WIFI_CONNECTION);
		return wifi.removeEntry(entry);
	}

	@Override
	public boolean reconfigure() throws IOException {
		privileges.check(Privileges.RECONFIGURE_WIFI);
		return wifi.reconfigure();
	}

	@Override
	public WifiStatus status() throws IOException {
		privileges.check(Privileges.KNOW_WIFI_STATUS);
		return wifi.status();
	}

}