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
package com.superdashi.gosper.http;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.StreamException;
import com.tomgibara.streams.Streams;

import fi.iki.elonen.NanoHTTPD.Response.Status;

public final class HttpUtil {

	private static final int DEFAULT_REQUEST_BYTES_SIZE = 1024;
	private static final int DEFAULT_REQUEST_BUFFER_SIZE = 1024;
	private static final int MAXIMUM_REQUEST_BYTES_SIZE = 1024 * 1024;
	private static final Pattern SEMICOLON = Pattern.compile(";\\s*");

	static int initialCapacity(HttpRequest request) {
		long contentLength = request.contentLength();
		return contentLength >= 0 && contentLength <= Integer.MAX_VALUE ?
				(int) contentLength : DEFAULT_REQUEST_BYTES_SIZE;
	}

	static StreamBytes requestBytes(int initialCapacity) {
		return Streams.bytes(initialCapacity, MAXIMUM_REQUEST_BYTES_SIZE);
	}

	static String contentAsString(HttpRequest request) throws StreamException {
		try (Reader r = request.contentAsReader()) {
			final CharBuffer buffer = CharBuffer.allocate(DEFAULT_REQUEST_BUFFER_SIZE);
			final int limit = MAXIMUM_REQUEST_BYTES_SIZE / 2; // assumes 2 bytes per character
			int total = 0;
			StringBuilder sb = new StringBuilder(initialCapacity(request)); // assumes 1 byte per character
			while (true) {
				int read = r.read(buffer);
				if (read == -1) break;
				total += read;
				if (total > limit) throw new StreamException("request exceeded size limit");
				buffer.flip();
				sb.append(buffer, 0, read);
			}
			return sb.toString();
		} catch (IOException e) {
			throw new StreamException(e);
		}

	}

	static String mimeType(String ct) {
		if (ct == null) return null;
		int i = ct.indexOf(';');
		return i == -1 ? ct : ct.substring(0, i);
	}

	static String contentType(String ct) {
		if (ct == null) return null;
		int i = ct.indexOf(';');
		if (i == -1) return null;
		String[] parts = SEMICOLON.split(ct);
		for (int j = 1; j < parts.length; j++) {
			String part = parts[i];
			if (part.startsWith("charset=")) {
				//TODO should handle quoted strings
				return part.substring(8);
			}
		}
		return null;
	}

	static ZonedDateTime parseHttpDate(String str) {
		if (str == null) return null;
		return ZonedDateTime.parse(str, DateTimeFormatter.RFC_1123_DATE_TIME);
	}

	static int validStatusCode(int code) {
		return code < 100 || code >= 600 ? 500 : code;
	}

	static String defaultStatusMessage(int code) {
		//TODO this method is actually very inefficiently implemented
		Status status = Status.lookup(code);
		return status == null ? "Unknown" : status.getDescription();
	}

	private HttpUtil() {}

}
