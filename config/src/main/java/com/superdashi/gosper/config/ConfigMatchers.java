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
