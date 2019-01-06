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
