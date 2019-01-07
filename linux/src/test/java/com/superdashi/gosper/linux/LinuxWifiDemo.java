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
