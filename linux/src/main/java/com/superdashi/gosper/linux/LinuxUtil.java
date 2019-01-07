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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;

class LinuxUtil {

	private static int width = 0;

	static String readFully(Process proc) {
		try (ReadStream read = Streams.streamInput(proc.getInputStream())) {
			StreamBytes bytes = Streams.bytes();
			read.to(bytes.writeStream()).transferFully();
			return new String(bytes.bytes());
		}
	}

	static List<String> readFullyAsLines(Process proc) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
			String line;
			while (null != (line = r.readLine())) {
				lines.add(line);
			}
		}
		return lines;
	}
	static String unescape(String str) {
		return str.replace("\\\"", "\"");
	}

	static boolean isQuoted(String str) {
		int len = str.length();
		return len >= 2 && str.charAt(0) == '"' && str.charAt(len - 1) == '"';
	}

	static String unquote(String str) {
		return isQuoted(str) ? unescape(str.substring(1, str.length() - 1)) : str;
	}

	static String quote(String ssid) {
		return '"' + ssid.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"") + '"';
	}

	static boolean is64Bit() {
		if (width == 0) {
			String arch = System.getProperty("os.arch");
			width = arch.contains("64") ? 1 : -1;
		}
		return width > 0;
	}
}
