package com.superdashi.gosper.graphdb;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

// limit is exclusive
final class MappedIntIterator<S> implements PrimitiveIterator.OfInt {

	@FunctionalInterface
	interface Mapper<S> {
		int apply(S obj);
	}

	private final Iterator<S> src;
	private final Mapper<S> map;
	private final S lim;
	private S next;

	MappedIntIterator(Iterator<S> src, Mapper<S> map, S lim) {
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
	public int nextInt() {
		if (next == null) throw new NoSuchElementException();
		int ret = map.apply(next);
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
