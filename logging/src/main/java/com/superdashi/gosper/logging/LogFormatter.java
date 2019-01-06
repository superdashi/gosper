package com.superdashi.gosper.logging;

import java.io.IOException;

public interface LogFormatter {

	static LogFormatter simple() { return Logging.simpleFormatter; }

	<T> LogAppender<T> format(LogAppender<T> appender, LogEntry entry) throws IOException;
}
