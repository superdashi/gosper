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
package com.superdashi.gosper.graphdb;

import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.Stream;

// collects ids of parts that have changed
//TODO need to guard against overflow - perhaps move to disk??
final class Queue {

	private static final long[] NO_KEYS = {};

	private long[] keys = NO_KEYS;
	private int length = 0;

	void enqueue(Part part) {
		long key = EdgeKey.id(part.edgeId(), part.sourceId());
		if (length == keys.length) {
			keys = length == 0 ? new long[16] : Arrays.copyOfRange(keys, 0, length * 2);
		}
		keys[length++] = key;
	}

	void compact() {
		if (keys.length > 0) {
			Arrays.sort(keys, 0, length);
			int j = 1;
			for (int i = 1; i < length; i++) {
				if (keys[i] == keys[j-1]) continue; // don't advance j
				if (i != j) keys[j] = keys[i];
				j++;
			}
			length = j;
			if (length < keys.length) {
				keys = Arrays.copyOfRange(keys, 0, length);
			}
		}
	}

	Stream<PartRef> stream() {
		switch (length) {
		case 0 : return Stream.empty();
		case 1 : return Stream.of(new PartRef(keys[0]));
		default: return LongStream.of(keys).limit(length).mapToObj(id -> new PartRef(id));
		}
	}

	boolean empty() {
		return length == 0;
	}

}
