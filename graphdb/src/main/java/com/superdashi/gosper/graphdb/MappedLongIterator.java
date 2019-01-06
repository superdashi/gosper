package com.superdashi.gosper.graphdb;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

// limit is exclusive
final class MappedLongIterator<S> implements PrimitiveIterator.OfLong {

	@FunctionalInterface
	interface Mapper<S> {
		long apply(S obj);
	}

	private final Iterator<S> src;
	private final Mapper<S> map;
	private final S lim;
	private S next;

	MappedLongIterator(Iterator<S> src, Mapper<S> map, S lim) {
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
	public long nextLong() {
		if (next == null) throw new NoSuchElementException();
		long ret = map.apply(next);
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
