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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

// limit is exclusive
final class MappedIterator<S,T> implements Iterator<T> {

	private final Iterator<S> src;
	private final Function<S,T> map;
	private final S lim;
	private S next;

	MappedIterator(Iterator<S> src, Function<S,T> map, S lim) {
		this.src = src;
		this.map = map;
		this.lim = lim;
		advance();
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public T next() {
		if (next == null) throw new NoSuchElementException();
		T ret = map.apply(next);
		advance();
		return ret;
	}

	private void advance() {
		if (src.hasNext()) {
			next = src.next();
			if (lim != null && next != null && next.equals(lim)) next = null;
		} else {
			next = null;
		}
	}
}
