package com.superdashi.gosper.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.function.Function;

// primarily present to make time related functions available to JS
public final class Time {

	public enum FormatStyle {

		FULL(java.time.format.FormatStyle.FULL),
		LONG(java.time.format.FormatStyle.LONG),
		MEDIUM(java.time.format.FormatStyle.MEDIUM),
		SHORT(java.time.format.FormatStyle.SHORT);

		final java.time.format.FormatStyle style;

		private FormatStyle(java.time.format.FormatStyle style) {
			this.style = style;
		}
	}

	public static Instant now() {
		return Instant.now();
	}

	private final Locale locale;
	private final ZoneId zoneId;

	public Function<Instant, String> patternFormatter(String pattern) {
		if (pattern == null) throw new IllegalArgumentException("null pattern");
		DateTimeFormatter f = DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId);
		return f::format;
	}

	public Function<Instant, String> dateFormatter(FormatStyle dateStyle) {
		if (dateStyle == null) throw new IllegalArgumentException("null dateStyle");
		DateTimeFormatter f = DateTimeFormatter.ofLocalizedDate(dateStyle.style).withLocale(locale).withZone(zoneId);
		return f::format;
	}

	public Function<Instant, String> timeFormatter(FormatStyle timeStyle) {
		if (timeStyle == null) throw new IllegalArgumentException("null timeStyle");
		DateTimeFormatter f = DateTimeFormatter.ofLocalizedTime(timeStyle.style).withLocale(locale).withZone(zoneId);
		return f::format;
	}

	public Function<Instant, String> dateTimeFormatter(FormatStyle dateStyle, FormatStyle timeStyle) {
		if (dateStyle == null) throw new IllegalArgumentException("null dateStyle");
		if (timeStyle == null) throw new IllegalArgumentException("null timeStyle");
		DateTimeFormatter f = DateTimeFormatter.ofLocalizedDateTime(dateStyle.style, timeStyle.style).withLocale(locale).withZone(zoneId);
		return f::format;
	}

	public Time() {
		locale = Locale.getDefault(Category.FORMAT);
		zoneId = ZoneId.systemDefault();
	}

	public Time(Locale locale, ZoneId zoneId) {
		this.locale = locale;
		this.zoneId = zoneId;
	}
}
