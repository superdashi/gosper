package com.superdashi.gosper.config;

import java.util.regex.Pattern;


public class ConfigMatchers {

	public static ConfigMatcher all() {
		return v -> true;
	}

	public static ConfigMatcher notEmpty() {
		return v -> v != null && !v.isEmpty();
	}

	public static ConfigMatcher equals(String value) {
		if (value == null) throw new IllegalArgumentException("null value");
		return value.isEmpty() ? all() : v -> value.equals(v);
	}

	public static ConfigMatcher startsWith(String value) {
		if (value == null) throw new IllegalArgumentException("null value");
		return value.isEmpty() ? all() : v -> v != null && v.startsWith(value);
	}

	public static ConfigMatcher endsWith(String value) {
		if (value == null) throw new IllegalArgumentException("null value");
		return value.isEmpty() ? all() : v -> v != null && v.endsWith(value);
	}

	public static ConfigMatcher contains(String value) {
		if (value == null) throw new IllegalArgumentException("null value");
		return value.isEmpty() ? all() : v -> v != null && v.contains(value);
	}

	public static ConfigMatcher matches(String value) {
		if (value == null) throw new IllegalArgumentException("null value");
		Pattern p = Pattern.compile(value);
		return value.isEmpty() ? all() : v -> v != null && p.matcher(v).matches();
	}

}
