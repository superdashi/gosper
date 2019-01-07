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

public interface LogRecorder {

	// a recorder that disposes of all entries
	static LogRecorder devnull() {
		return e -> {};
	}

	static LogRecorder syserr(LogFormatter formatter) {
		return e -> formatter.format(Logging.syserrAppender, e);
	}

	static LogRecorder syserr() {
		return e -> Logging.simpleFormatter.format(Logging.syserrAppender, e);
	}

	static LogRecorder sysout(LogFormatter formatter) {
		return e -> formatter.format(Logging.sysoutAppender, e);
	}

	static LogRecorder sysout() {
		return e -> Logging.simpleFormatter.format(Logging.sysoutAppender, e);
	}

	static LogRecorder sys() {
		return e -> {
			LogAppender<?> appender = e.level.ordinal() < LogLevel.INFO.ordinal() ? Logging.syserrAppender : Logging.sysoutAppender;
			Logging.simpleFormatter.format(appender, e);
		};
	}

	void record(LogEntry entry) throws IOException;
}
