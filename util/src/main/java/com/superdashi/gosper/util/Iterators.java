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

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class Iterators {

	public static <E> Iterator<E> empty() {
		return Collections.emptyIterator();
	}

	public static <E> Iterator<E> array(E... es) {
		if (es == null) throw new IllegalArgumentException("null es");
		return new Iterator<E> () {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < es.length;
			}

			@Override
			public E next() {
				if (index == es.length) throw new NoSuchElementException();
				return es[index++];
			}

		};
	}

	public static <E> Iterator<E> flatten(Iterator<Iterator<E>> its) {
		if (its == null) throw new IllegalArgumentException("null its");
		CombinedIterator<E> it = new CombinedIterator<>(its);
		// optimization: if its has no more iterators after construction, just use last one directly
		return its.hasNext() ? it : it.next == null ? empty() : it.next;
	}

	public static <E> Iterator<E> sequence(Iterator<E>... its) {
		switch (its.length) {
		case 0 : return Collections.emptyIterator();
		case 1 : return its[0];
		case 2 : return sequence(its[0], its[1]);
		default:
			return flatten(array(its));
		}
	}

	public static <E> Iterator<E> sequence(Iterator<E> i1, Iterator<E> i2) {
		if (i1 == null) throw new IllegalArgumentException("null i1");
		if (i2 == null) throw new IllegalArgumentException("null i2");
		if (!i1.hasNext()) return i2;
		if (!i2.hasNext()) return i1;
		return new Iterator<E>() {

			Iterator<E> prev = null;
			Iterator<E> next = i1;

			@Override
			public boolean hasNext() {
				return next != null && next.hasNext();
			}

			@Override
			public E next() {
				E e = next.next();
				prev = next;
				if (!next.hasNext()) {
					if (next == i1) {
						next = i2;
					} else {
						next = null;
					}
				}
				return e;
			}

			@Override
			public void remove() {
				if (prev == null) throw new IllegalStateException();
				prev.remove();
				prev = null;
			}

		};
	}

	public static <E,F> Iterator<F> map(Iterator<E> it, Function<E,F> f) {
		return new Iterator<F>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public F next() {
				return f.apply(it.next());
			}

			@Override
			public void remove() {
				it.remove();
			}

		};
	}

	private static class CombinedIterator<E> implements Iterator<E> {

		private final Iterator<Iterator<E>> its;

		Iterator<E> prev;
		Iterator<E> next = null;

		CombinedIterator(Iterator<Iterator<E>> its) {
			this.its = its;
			advance();
		}

		@Override
		public boolean hasNext() {
			return next != null && next.hasNext();
		}

		@Override
		public E next() {
			if (!hasNext()) throw new NoSuchElementException();
			E e = next.next();
			advance();
			return e;
		}

		@Override
		public void remove() {
			if (prev == null)  throw new IllegalStateException();
			prev.remove();
			prev = null;
		}

		private void advance() {
			Iterator<E> i = next;
			while (next == null || !next.hasNext()) {
				if (its.hasNext()) {
					next = its.next();
				} else {
					next = null;
					break;
				}
			}
			prev = i;
		}
	}
}
