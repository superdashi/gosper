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
