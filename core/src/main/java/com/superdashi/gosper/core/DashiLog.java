package com.superdashi.gosper.core;

import java.text.MessageFormat;

//TODO this all needs doing properly
public class DashiLog {

	public static enum Level {
		NONE, WARN, INFO, DEBUG, TRACE;

		String prefix;

		Level() {
			String name = name();
			prefix = name.length() == 4 ? name + "  " : name + " ";
		}
	}

	private static Level level = Level.INFO;
	private static boolean warn  = true;
	private static boolean info  = true;
	private static boolean debug = false;
	private static boolean trace = false;

	static {
		String levelName = System.getProperty("dashi.logLevel");
		if (levelName != null && !levelName.isEmpty()) setLevel(levelName);
	}

	public static boolean isWarn() {
		return warn;
	}

	public static boolean isInfo() {
		return info;
	}

	public static boolean isDebug() {
		return debug;
	}

	public static boolean isTrace() {
		return trace;
	}

	public static void setLevel(String levelName) {
		Level newLevel;
		try {
			newLevel = Level.valueOf(levelName.toUpperCase());
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid logging level ignored: " + levelName);
			return;
		}
		setLevel(newLevel);
	}

	public static void setLevel(Level newLevel) {
		if (newLevel == level) return;
		int ord = newLevel.ordinal();
		warn  = ord >= 1;
		info  = ord >= 2;
		debug = ord >= 3;
		trace = ord >= 4;
		level = newLevel;
		info("debug level changed to {0}", newLevel);
	}

	public static void warn(String message) {
		if (warn) record(Level.WARN, message);
	}

	public static void warn(String message, Throwable t) {
		if (warn) record(Level.WARN, t, message);
	}

	public static void warn(String message, Object... args) {
		if (warn) record(Level.WARN, format(message, args));
	}

	public static void warn(String message, Throwable t, Object... args) {
		if (warn) record(Level.WARN, t, format(message, args));
	}

	public static void info(String message) {
		if (info) record(Level.INFO, message);
	}

	public static void info(String message, Object... args) {
		if (info) record(Level.INFO, format(message, args));
	}

	public static void debug(String message) {
		if (debug) record(Level.DEBUG, message);
	}

	public static void debug(String message, Object... args) {
		if (debug) record(Level.DEBUG, format(message, args));
	}

	public static void debug(String message, Throwable t, Object... args) {
		if (debug) record(Level.DEBUG, t, format(message, args));
	}

	public static void trace(String message) {
		if (trace) record(Level.TRACE, message);
	}

	public static void trace(String message, Object... args) {
		if (trace) record(Level.TRACE, format(message, args));
	}

	// trace calls may be called extremely tightly - avoid autoboxing in common cases

	public static void trace(String message, int value) {
		if (trace) record(Level.TRACE, format(message, value));
	}

	public static void trace(String message, float value) {
		if (trace) record(Level.TRACE, format(message, value));
	}

	public static void trace(String message, String value) {
		if (trace) record(Level.TRACE, format(message, value));
	}

	public static void trace(String message, Object value) {
		if (trace) record(Level.TRACE, format(message, value));
	}

	// private helper methods

	private static String format(String message, Object... args) {
		return MessageFormat.format(message, args);
	}

	private static void record(Level level, String message) {
		System.out.print(level.prefix);
		System.out.println(message);
	}

	private static void record(Level level, Throwable t, String message) {
		System.out.print(level.prefix);
		System.out.println(message);
		t.printStackTrace(System.out);
	}

}
