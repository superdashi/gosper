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
