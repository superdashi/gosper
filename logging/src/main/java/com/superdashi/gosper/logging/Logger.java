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
package com.superdashi.gosper.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.storage.Stores;
import com.tomgibara.bits.Bits;

public final class Logger {

	//NOTE: made into constants here for efficiency: must be consistent with LogLevel

	private static final int ORD_ERROR   = 0;
	private static final int ORD_WARNING = 1;
	private static final int ORD_INFO    = 2;
	private static final int ORD_DEBUG   = 3;

	private static final Pattern LINE_SPLITTER = Pattern.compile("\\r?\\n");

	private static List<String> convert(Throwable stacktrace) {
		if (stacktrace == null) return null;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		stacktrace.printStackTrace(pw);
		return Collections.unmodifiableList(Arrays.asList(LINE_SPLITTER.split(sw.toString())));
	}

	// method exposed to perform substitution directly
	static String substitute(String message, Object... values) {
		return new Sub(message).apply(values);
	}

	private final Logging noLogging = new Logging();
	// the domain to which log entries are reported
	private final LogDomain domain;
	private final LogIdentity identity;
	private final LogQueue logQueue;
	private volatile int requiredLevel; // ordinal of required level, or -1 indicates nothing logged
	private volatile long nextId = 0L;

	Logger(LogDomain domain, LogIdentity identity, LogQueue logQueue, LogLevel level) {
		this.domain = domain;
		this.identity = identity;
		this.logQueue = logQueue;
		requiredLevel(level);
	}

	public Logger child(String name) {
		return domain.loggerFor(identity.child(name));
	}

	public Logger descendant(String... names) {
		return domain.loggerFor(identity.descendant(names));
	}

	public Logging at(LogLevel level) {
		if (level == null) throw new IllegalArgumentException("null level");
		return loggingForLevel(level);
	}

	public boolean isLogged(LogLevel level) {
		if (level == null) throw new IllegalArgumentException("null level");
		return level.ordinal() <= requiredLevel;
	}

	public boolean isErrorLogged() {
		return ORD_ERROR <= requiredLevel;
	}

	public boolean isWarningLogged() {
		return ORD_WARNING <= requiredLevel;
	}

	public boolean isInfoLogged() {
		return ORD_INFO <= requiredLevel;
	}

	public boolean isDebugLogged() {
		return ORD_DEBUG <= requiredLevel;
	}

	public Logging error() {
		return loggingForLevel(LogLevel.ERROR);
	}

	public Logging warning() {
		return loggingForLevel(LogLevel.WARNING);
	}

	public Logging info() {
		return loggingForLevel(LogLevel.INFO);
	}

	public Logging debug() {
		return loggingForLevel(LogLevel.DEBUG);
	}

	private Logging loggingForLevel(LogLevel level) {
		return requiredLevel < level.ordinal() ? noLogging : new Logging(level);
	}

	public class Logging {

		private final LogEntry entry;
		private Object[] values = null;
		private Throwable stacktrace = null;

		// only used for non-logger
		private Logging() {
			entry = new LogEntry(identity, -1L, LogLevel.DEBUG);
		}

		private Logging(LogLevel level) {
			entry = new LogEntry(identity, nextId++, level);
		}

		// negative values treated as default
		public Logging timestamp(long timestamp) {
			if (timestamp >= 0) entry.timestamp = timestamp;
			return this;
		}

		public Logging filePath(String filePath) {
			entry.filePath = filePath;
			return this;
		}

		public Logging filePath(Path filePath) {
			entry.filePath = filePath == null ? null : filePath.toString();
			return this;
		}

		public Logging filePath(URI uri) {
			entry.filePath = uri == null ? null : uri.toString();
			return this;
		}

		// negative line numbers are ignored
		public Logging lineNumber(int lineNumber) {
			if (lineNumber >= 0) entry.lineNumber = lineNumber;
			return this;
		}

		public Logging message(String message) {
			entry.message = message;
			return this;
		}

		public Logging values(Object... values) {
			if (this != noLogging) this.values = values;
			return this;
		}

		public Logging stacktrace(Throwable stacktrace) {
			this.stacktrace = stacktrace;
			return this;
		}

		public boolean log() {
			// check if this is already rejected
			if (this == noLogging) return false;
			// ensure the entry has a timestamp
			entry.ensureTimestamp();
			// convert the stacktrace
			entry.stacktrace = convert(stacktrace);
			// substitute values into message if necessary
			if (values != null && values.length != 0 && entry.message != null) {
				entry.message = new Sub(entry.message).apply(values);
			}
			return logQueue.add(entry);
		}

	}

	LogIdentity identity() {
		return identity;
	}

	void requiredLevel(LogLevel level) {
		requiredLevel = level == null ? -1 : level.ordinal();
	}

	private static final class Sub {
		private static final int MARKER_LIMIT = 64;
		private final char[] string;
		private final int[] subs; //-ve is number of characters to chop from string, +ve is index into combined with 3 bits for number of characters to skip

		private Sub(String message) {
			char[] cs = message.toCharArray();
			int length = cs.length; // the number of characters in the string
			int[] subs = new int[length]; // subs cannot be long than the original string, and is likely to be very much shorter
			int index = 0; // the lookup index
			boolean auto = false; // whether we've located any auto indexed markers
			int start = 0; // start of textual segment
			int end = -1; // end of textual segment
			int j = 0; // index into the subs array
			for (int i = 0; i < length; i++) {
				char c = cs[i];
				if (end < 0) { // we're looking for a marker
					if (c == '{') { // we have found one
						index = 0; // start the indexing
						end = i; // record where the previous segment ended
					}
				} else { // we're looking for a marker to end
					if (c == '}') { // we've found one
						if (end != start) { // there is text we need to record
							subs[j++] = start - end; // record negative value to differentiate from marker record
						}
						int width = i - end + 1; // number of characters since end of last textual segment
						if (width == 2) { // this is an auto-numbered one, set as a rogue value and assign a number later
							subs[j++] = Integer.MIN_VALUE; // this value cannot be achieved as the length of a textual segment
							auto = true; // recording presence of an auto means no unnecessary checking later
						} else { // bit-pack the width, and the index
							subs[j++] = (index << 3) | width;
							index = 0; // reset the index in preparation another possible marker
						}
						end = -1; // this marker is done
						start = i + 1; // record where the next text is expected to start
					} else if (c >= '0' && c <='9') { // we've found another digit
						int width = i - end - 1; // the number of digits so far
						if (width == 1 && index == 0) { // we're strict - we don't allow leading zeros
							end = -1; // bail-out of this marker and keep looking
						} else {
							index = 10 * index + (c - 48); // update the index value, and we keep looking for a marker close
							if (index >= MARKER_LIMIT) { // we only allow up to this many markers
								end = -1; // bail-out of this marker and keep looking
								index = 0; // reset the index in preparation another possible marker
							}
						}
					} else { // this character cannot be in a marker, we must keep looking
						end = -1; // bail-out of this marker and keep looking
						index = 0; // reset the index in preparation another possible marker
					}
				}
			}
			if (start != length) { // tack-on the last bit of text
				subs[j++] = start - length;
			}
			if (auto) {
				BitStore bits = Bits.store(MARKER_LIMIT);
				// first identify the used indices
				for (int i = 0; i < j; i++) {
					int s = subs[i];
					if (s >= 0) { // we've found a known marker
						bits.setBit(s >> 3, true);
					}
				}
				// now auto index the remainders
				int overflow = 0; // in case of a runaway number of autos
				BitMatches zeros = bits.zeros();
				for (int i = 0; i < j; i++) {
					if (subs[i] == Integer.MIN_VALUE) {
						int next = zeros.first();
						if (next == MARKER_LIMIT) {
							next = MARKER_LIMIT + overflow++;
						} else {
							bits.setBit(next, true); // record that this index is now used up
						}
						subs[i] = (next << 3) | 2;
					}
				}
			}
			this.string = cs;
			this.subs = Arrays.copyOfRange(subs, 0, j);
		}

		String apply(Object... values) {
			int length = values.length;
			StringBuilder sb = new StringBuilder(string.length);
			int from = 0;
			for (int sub : subs) {
				if (sub < 0) {
					sb.append(string, from, -sub);
					from -= sub;
				} else {
					from += sub & 7;
					int i = sub >> 3;
					if (i < length) {
						Object value = values[i];
						String str;
						if (value == null) {
							str = "null";
						} else if (value.getClass().isArray()) {
							str = Stores.values(value).toString();
						} else {
							str = value.toString();
						}
						sb.append(str);
					}
				}
			}
			return sb.toString();
		}
	}
}
