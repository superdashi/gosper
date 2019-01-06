package com.superdashi.gosper.graphdb;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

//TODO remove
interface Sequence<T extends Comparable> extends Iterable<T> {

	default public Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	default public Sequence<T> and(Sequence<T> that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return () -> {
			return new Iterator<T>() {
				private Iterator<T> itA = iterator();
				private Iterator<T> itB = that.iterator();
				private T next;

				{ advance(); }

				@Override
				public boolean hasNext() {
					return next != null;
				}

				@Override
				public T next() {
					if (next == null) throw new NoSuchElementException();
					T ret = next;
					advance();
					return ret;
				}

				private void advance() {
					next = null;
					if (!itA.hasNext() || !itB.hasNext()) return;
					T a = itA.next();
					T b = itB.next();
					while (true) {
						int c = a.compareTo(b);
						if (c == 0) {
							next = a;
							return;
						} else if (c < 0) {
							if (!itA.hasNext()) return;
							a = itA.next();
						} else {
							if (!itB.hasNext()) return;
							b = itB.next();
						}
					}
				}
			};
		};
	}

	//TODO could optimize by recording that one of the iterators is exhausted
	default public Sequence<T> or(Sequence<T> that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return () -> {
			return new Iterator<T>() {
				private Iterator<T> itA = iterator();
				private Iterator<T> itB = that.iterator();
				private T nextA;
				private T nextB;

				{ advance(); }

				@Override
				public boolean hasNext() {
					return nextA != null || nextB != null;
				}

				@Override
				public T next() {
					if (nextA == null && nextB == null) throw new NoSuchElementException();
					T ret;
					if (nextA == null) {
						ret = nextB;
						nextB = null;
					} else if (nextB == null) {
						ret = nextA;
						nextA = null;
					} else {
						int c = nextA.compareTo(nextB);
						if (c == 0) {
							ret = nextA;
							nextA = null;
							nextB = null;
						} else if (c < 0) {
							ret = nextA;
							nextA = null;
						} else {
							ret = nextB;
							nextB = null;
						}
					}
					advance();
					return ret;
				}

				private void advance() {
					if (nextA == null && itA.hasNext()) nextA = itA.next();
					if (nextB == null && itB.hasNext()) nextB = itB.next();
				}
			};
		};
	}

	default public Sequence<T> filter(Predicate<T> predicate) {
		if (predicate == null) throw new IllegalArgumentException("null predicate");
		return () -> {
			return new Iterator<T>() {
				private Iterator<T> it = iterator();
				private T next;

				{ advance(); }

				@Override
				public boolean hasNext() {
					return next != null;
				}

				@Override
				public T next() {
					if (next == null) throw new NoSuchElementException();
					T ret = next;
					advance();
					return ret;
				}

				private void advance() {
					next = null;
					while (it.hasNext()) {
						T candidate = it.next();
						if (predicate.test(candidate)) {
							next = candidate;
							break;
						}
					}
				}
			};
		};
	}
}
