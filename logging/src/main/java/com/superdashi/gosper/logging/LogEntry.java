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

import java.io.IOException;
import java.util.List;

public final class LogEntry {

	final LogIdentity logger;
	final long id;
	final LogLevel level;

	long timestamp = -1L;
	String filePath;
	int lineNumber;
	String message;
	List<String> stacktrace;

	LogEntry(LogIdentity logger, long id, LogLevel level) {
		this.logger = logger;
		this.id = id;
		this.level = level;
	}

	// accessors

	public LogIdentity logger() {
		return logger;
	}

	public long id() {
		return id;
	}

	public LogLevel level() {
		return level;
	}

	public long timestamp() {
		return timestamp;
	}

	public String filePath() {
		return filePath;
	}

	public int lineNumber() {
		return lineNumber;
	}

	public String message() {
		return message;
	}

	public List<String> stacktrace() {
		return stacktrace;
	}

	// package scoped methods

	void ensureTimestamp() {
		if (timestamp == -1L);
		timestamp = System.currentTimeMillis();
	}

	// object methods

	@Override
	public String toString() {
		try {
			return Logging.simpleFormatter.format(LogAppender.over(new StringBuilder()), this).underlying().toString();
		} catch (IOException e) {
			// should be impossible
			throw new RuntimeException(e);
		}
	}
}
