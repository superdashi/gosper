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
package com.superdashi.gosper.util;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This class may be used to convert between a textual representation for a unit
 * of time and its millisecond equivalent or vice versa. Instances are immutable
 * and are safe for concurrent use.
 * </p>
 *
 * <p>
 * The duration format parsable by this class consists of list of positively
 * valued fields in descending order of unit magnitude. The recognized field
 * units are:
 * </p>
 *
 * <ul>
 * <li>Days - unit: d, day or days</li>
 * <li>Hours - unit: h, hr, hrs, hour, hours</li>
 * <li>Minutes - unit: m, min, mins, minute, minutes</li>
 * <li>Seconds - unit: s, sec, secs, second, seconds</li>
 * <li>Milliseconds - unit: ms, milli, millis, millisecond, milliseconds</li>
 * </ul>
 *
 * <p>
 * Spaces may appear between fields but not between numbers and units. Unit
 * plurality is inconsequential. A string containing no units (eg. the empty
 * string) may be used to represent zero milliseconds.
 * </p>
 *
 * <p>
 * Examples of valid durations:
 * </p>
 *
 * <ul>
 * <li>
 *
 * <pre>
 *    4days 10hours
 * </pre>
 *
 * long units</li>
 * <li>
 *
 * <pre>
 *    10m2s500ms
 * </pre>
 *
 * short units</li>
 * <li>
 *
 * <pre>
 *    10m2s500ms
 * </pre>
 *
 * mixed units</li>
 * <li>
 *
 * <pre>
 *    2hour 1seconds
 * </pre>
 *
 * inconsequential plurality</li>
 * <li>
 *
 * <pre>
 *    1day 25hrs
 * </pre>
 *
 * value exceeding prior field</li>
 * </ul>
 *
 * <p>
 * To convert a string into milliseconds:
 *
 * <pre>
 * new Duration(str).getTime()
 * </pre>; to convert a time in milliseconds into a string:
 *
 * <pre>
 * new Duration(time).toString()
 * </pre>.
 * </p>
 *
 * @author Tom Gibara
 */

public final class Duration implements Comparable<Duration>, Serializable {

	// statics

	/**
	 * Serialization id.
	 */
	private static final long serialVersionUID = -9128850324443225029L;

	/**
	 * The pattern which all string representations are required to match.
	 */

	private static final Pattern PATTERN = Pattern.compile("\\s*(?:(\\d+)(?:d|day|days)\\s*)?"
			+ "(?:(\\d+)(?:h|hr|hrs|hour|hours)\\s*)?" + "(?:(\\d+)(?:m|min|mins|minute|minutes)\\s*)?"
			+ "(?:(\\d+)(?:s|sec|secs|second|seconds)\\s*)?"
			+ "(?:(\\d+)(?:ms|milli|millis|millisecond|milliseconds)\\s*)?", Pattern.CASE_INSENSITIVE);

	/**
	 * Converts a field from the string constructor into a long.
	 *
	 * @param matcher
	 *            a matcher which has matched PATTERN
	 * @param index
	 *            the captured group containing a long value
	 * @param name
	 *            the name of the duration field for error reporting
	 * @return the index group of the matcher as a long
	 *
	 * @throws IllegalArgumentException
	 *             if the captured group is not parsable as a long (this should
	 *             be impossible)
	 */

	private static long parseLong(final Matcher matcher, final int index, final String name)
			throws IllegalArgumentException {
		String str = matcher.group(index);
		try {
			return str == null ? 0L : Long.parseLong(str);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format("%s not a number: %s", name, str), e);
		}
	}

	public static long parseMillis(String string) throws IllegalArgumentException {
		if (string == null) throw new IllegalArgumentException("null string");
		Matcher matcher = PATTERN.matcher(string);
		if (!matcher.matches()) throw new IllegalArgumentException(String.format("%s does not match %s", "string",
				matcher.pattern().pattern()));

		long days = parseLong(matcher, 1, "days");
		long hours = parseLong(matcher, 2, "hours");
		long minutes = parseLong(matcher, 3, "minutes");
		long seconds = parseLong(matcher, 4, "seconds");
		long milliseconds = parseLong(matcher, 5, "milliseconds");

		return milliseconds + 1000L * (seconds + 60L * (minutes + 60L * (hours + (24L * days))));
	}

	// fields

	/**
	 * The string which was passed to the constructor, or a generated value
	 * which would evaluate to the number supplied to the long constructor.
	 */

	private String string;

	/**
	 * The long which was passed to the constructor, or the evaluation of the
	 * string which was supplied to the constructor.
	 */
	private long time;

	// constructors

	/**
	 * Creates a duration from its string representation.
	 *
	 * @param string
	 *            the string representation of a duration
	 * @throws IllegalArgumentException
	 *             if the string is null or does not match the required format.
	 */

	public Duration(final String string) throws IllegalArgumentException {
		this.time = parseMillis(string);
		this.string = string;
	}

	/**
	 * Creates a duration from a number of milliseconds.
	 *
	 * @param time
	 *            a time in milliseconds
	 * @throws IllegalArgumentException
	 *             if the supplied time is negative
	 */

	public Duration(final long time) throws IllegalArgumentException {
		if (time < 0L) throw new IllegalArgumentException("negative time");
		long t = time;
		long milliseconds = t % 1000L;
		t /= 1000L;
		long seconds = t % 60;
		t /= 60;
		long minutes = t % 60;
		t /= 60;
		long hours = t % 24;
		t /= 24;
		long days = t;

		if (time == 0L) { // special case - empty string is legal, but
			// probably not preferred by clients
			string = "0s";
		} else {
			StringBuilder sb = new StringBuilder();
			if (days > 0L) sb.append(days).append(days == 1L ? "day" : "days");
			if (hours > 0L) sb.append(hours).append(hours == 1L ? "hour" : "hours");
			if (minutes > 0L) sb.append(minutes).append(minutes == 1L ? "min" : "mins");
			if (seconds > 0L) sb.append(seconds).append(seconds == 1L ? "second" : "secs");
			if (milliseconds > 0L) sb.append(milliseconds).append(milliseconds == 1L ? "milli" : "millis");
			this.string = sb.toString();
		}
		this.time = time;
	}

	// accessors

	/**
	 * The time which was passed to the constructor, or in the case that a
	 * string was supplied, the number of milliseconds to which the string
	 * equates.
	 *
	 * @return the duration in milliseconds
	 */

	public long getTime() {
		return time;
	}

	// comparable methods

	/**
	 * Durations ordered by time.
	 *
	 * @param obj
	 *            a duration object
	 * @return this is before, at or after obj
	 */

	public int compareTo(final Duration that) {
		if (this == that) return 0;
		return this.time < that.time ? -1 : this.time == that.time ? 0 : 1;
	}

	// object methods

	/**
	 * Equality is predicated on the millisecond time field. That is, two
	 * durations are equal if the value returned from their getTime() methods is
	 * equal.
	 *
	 * @param obj
	 *            the object to test for equality
	 * @return whether both objects represent the same duration
	 */

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Duration)) return false;
		Duration that = (Duration) obj;
		return this.time == that.time;
	}

	/**
	 * @return hashcode for this object based on the time
	 */

	@Override
	public int hashCode() {
		return (int) (time ^ (time >>> 32));
	}

	/**
	 * @return the string which was passed to the constructor or, in the case
	 *         that a time was supplied, a string which evalutes to an equal
	 *         duration.
	 */

	@Override
	public String toString() {
		return string;
	}

}
