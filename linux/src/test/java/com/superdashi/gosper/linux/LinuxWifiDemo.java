package com.superdashi.gosper.linux;
import java.io.IOException;
import java.util.List;

import com.superdashi.gosper.device.network.WifiAccessPoint;
import com.superdashi.gosper.linux.WPASupplicantWifi;
import com.superdashi.gosper.logging.LogDomain;
import com.superdashi.gosper.logging.LogRecorder;

public class LinuxWifiDemo {

	public static void main(String... args) throws IOException {
		LogDomain domain = new LogDomain(LogRecorder.sysout());
		WPASupplicantWifi wifi = new WPASupplicantWifi(domain.loggers().loggerFor("wifi"));
		List<WifiAccessPoint> scan = wifi.scan(Long.MAX_VALUE);
		scan.forEach(ap -> System.out.println(ap));
	}

}
