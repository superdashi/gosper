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
