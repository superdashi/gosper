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
package com.superdashi.gosper.micro;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

// simple wrapper around pattern
public final class Regex {

	// statics

	public enum Flag {
		UNIX_LINES(Pattern.UNIX_LINES),
		CASE_INSENSITIVE(Pattern.CASE_INSENSITIVE),
		COMMENTS(Pattern.COMMENTS),
		MULTILINE(Pattern.MULTILINE),
		LITERAL(Pattern.LITERAL),
		DOTALL(Pattern.UNICODE_CASE),
		UNICODE_CASE(Pattern.UNICODE_CASE),
		CANON_EQ(Pattern.CANON_EQ),
		UNICODE_CHARACTER_CLASS(Pattern.UNICODE_CHARACTER_CLASS)
		;

		final int value;

		private Flag(int value) {
			this.value = value;
		}

	}

	public static Regex compile(String pattern) throws IllegalArgumentException {
		if (pattern == null) throw new IllegalArgumentException("null pattern");
		return new Regex(Pattern.compile(pattern));
	}

	public static Regex compile(String pattern, Flag... flags) throws IllegalArgumentException {
		if (pattern == null) throw new IllegalArgumentException("null pattern");
		int sum = 0;
		for (Flag flag : flags) {
			if (flag == null) throw new IllegalArgumentException("null flag");
			if ((sum & flag.value) != 0) throw new IllegalArgumentException("duplicate flag");
			sum |= flag.value;
		}
		return new Regex(Pattern.compile(pattern, sum));
	}

	public static Regex deserialize(ReadStream r) {
		if (r == null) throw new IllegalArgumentException("null r");
		String pattern = r.readChars();
		int flags = r.readInt();
		return new Regex(Pattern.compile(pattern, flags));
	}

	// fields

	final Pattern pattern;

	// constructors

	private Regex(Pattern pattern) {
		this.pattern = pattern;
	}

	// public methods

	public boolean hasFlag(Flag flag) {
		if (flag == null) throw new IllegalArgumentException("null flag");
		return (pattern.flags() & flag.value) != 0;
	}

	public Set<Flag> flags() {
		EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
		int sum = pattern.flags();
		if (sum == 0) return Collections.emptySet();
		for (Flag flag : Flag.values()) {
			if ((sum & flag.value) != 0) set.add(flag);
		}
		return Collections.unmodifiableSet(set);
	}

	public void serialize(WriteStream w) {
		if (w == null) throw new IllegalArgumentException("null w");
		w.writeChars(pattern.pattern());
		w.writeInt(pattern.flags());
	}

	// object methods

	@Override
	public int hashCode() {
		return pattern.pattern().hashCode() + pattern.flags();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Regex)) return false;
		Regex that = (Regex) obj;
		return this.pattern.flags() == that.pattern.flags() && this.pattern.pattern().equals(that.pattern.pattern());
	}

	@Override
	public String toString() {
		return pattern.pattern();
	}
}
