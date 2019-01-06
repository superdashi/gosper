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
