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
