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
import java.io.PrintStream;
import java.util.Date;
import java.util.List;

final class Logging {

	static final String lineSeparator = String.format("%n");
	static final LogAppender<PrintStream> sysoutAppender = LogAppender.over(System.out);
	static final LogAppender<PrintStream> syserrAppender = LogAppender.over(System.err);
	static final LogFormatter simpleFormatter = new LogFormatter() {
		@Override
		public <T> LogAppender<T> format(LogAppender<T> appender, LogEntry entry) throws IOException {
			//TODO consider a better date format
			entry.logger().appendTo(appender).append('\t').append(entry.level()).append('\t').append(entry.id()).append("\t").append(entry.timestamp()).append("\t").append(new Date(entry.timestamp()));
			if (entry.message() != null) appender.append('\t').append(entry.message());
			if (entry.filePath() != null) {
				appender.append('\t').append(entry.filePath());
				if (entry.lineNumber() != 0) appender.append('[').append(entry.lineNumber()).append(']');
			}
			appender.appendNewline();
			List<String> stacktrace = entry.stacktrace();
			if (stacktrace != null) {
				for (String line : stacktrace) {
					appender.append('\t').append(line).appendNewline();
				}
			}
			return appender;
		}
	};

	private Logging() {}
}
