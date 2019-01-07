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
package com.superdashi.gosper.framework;

import java.util.Optional;

public enum Kind {

	APPLICATION,
	ACTIVITY,
	GRAPH_TYPE,
	GRAPH_ATTR,
	DATA_RECORDER,
	INFO_ACQUIRER,
	INFO_RENDERER;

	private static final int minLen;
	private static final int maxLen;

	static {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (Kind kind : Kind.values()) {
			String name = kind.name();
			int length = name.length();
			if (min > length) min = length;
			if (max < length) max = length;
		}
		minLen = min;
		maxLen = max;
	}

	public static Optional<Kind> optionalValueOf(String str) {
		// may avoid converting string case unnecessarily
		if (str.length() < minLen || str.length() > maxLen) return Optional.empty();
		//TODO could try some fancier optimization techniques
		return Optional.ofNullable(valueOfOrNull(str));
	}

	private static Kind valueOfOrNull(String str) {
		switch (str.toLowerCase()) {
		case "application"  : return APPLICATION  ;
		case "activity"     : return ACTIVITY     ;
		case "graph_type"   : return GRAPH_TYPE   ;
		case "graph_attr"   : return GRAPH_ATTR   ;
		case "data_recorder": return DATA_RECORDER;
		case "info_acquirer": return INFO_ACQUIRER;
		case "info_renderer": return INFO_RENDERER;
		default: return null;
		}
	}

	private final String str;

	private Kind() {
		str = name().toLowerCase();
	}

	@Override
	public String toString() {
		return str;
	}
}
