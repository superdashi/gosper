package com.superdashi.gosper.graphdb;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

// long contains nodeId (+) edge index
//TODO make functional on stream when moving to Java 9? (can use takeWhile)
interface EdgeSequence extends Iterable<Long> {

	static final PrimitiveIterator.OfLong emptyIterator = new PrimitiveIterator.OfLong() {
		@Override public boolean hasNext() { return false; }
		@Override public long nextLong() { throw new NoSuchElementException(); }
	};

	static final EdgeSequence empty = () -> emptyIterator;

	static PrimitiveIterator.OfLong singleIterator(long v) {
		return new PrimitiveIterator.OfLong() {
			private long next = v;
			@Override public boolean hasNext() { return next >= 0; }
			@Override public long nextLong() {
				if (next < 0) throw new NoSuchElementException();
				long ret = next;
				next = -1;
				return ret;
			}
		};
	}

	@Override
	PrimitiveIterator.OfLong iterator();

	default public LongStream stream() {
		Spliterator.OfLong spliterator = Spliterators.spliteratorUnknownSize(iterator(),
				Spliterator.DISTINCT  |
				Spliterator.IMMUTABLE |
				Spliterator.NONNULL   |
				Spliterator.ORDERED   |
				Spliterator.SORTED    );
		return StreamSupport.longStream(spliterator, false);
	}

	default public EdgeSequence and(EdgeSequence that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return () -> {
			return new PrimitiveIterator.OfLong() {
				private PrimitiveIterator.OfLong itA = iterator();
				private PrimitiveIterator.OfLong itB = that.iterator();
				private long next;

				{ advance(); }

				@Override
				public boolean hasNext() {
					return next >= 0;
				}

				@Override
				public long nextLong() {
					if (next < 0) throw new NoSuchElementException();
					long ret = next;
					advance();
					return ret;
				}

				private void advance() {
					next = -1;
					if (!itA.hasNext() || !itB.hasNext()) return;
					long a = itA.next();
					long b = itB.next();
					while (true) {
						int c = Long.compare(a, b);
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
	default public EdgeSequence or(EdgeSequence that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return () -> {
			return new PrimitiveIterator.OfLong() {
				private PrimitiveIterator.OfLong itA = iterator();
				private PrimitiveIterator.OfLong itB = that.iterator();
				private long nextA = -1;
				private long nextB = -1;

				{ advance(); }

				@Override
				public boolean hasNext() {
					return nextA >= 0 || nextB >= 0;
				}

				@Override
				public long nextLong() {
					if (nextA < 0 && nextB < 0) throw new NoSuchElementException();
					long ret;
					if (nextA < 0) {
						ret = nextB;
						nextB = -1;
					} else if (nextB < 0) {
						ret = nextA;
						nextA = -1;
					} else {
						int c = Long.compare(nextA, nextB);
						if (c == 0) {
							ret = nextA;
							nextA = -1;
							nextB = -1;
						} else if (c < 0) {
							ret = nextA;
							nextA = -1;
						} else {
							ret = nextB;
							nextB = -1;
						}
					}
					advance();
					return ret;
				}

				private void advance() {
					if (nextA < 0 && itA.hasNext()) nextA = itA.next();
					if (nextB < 0 && itB.hasNext()) nextB = itB.next();
				}
			};
		};
	}

}
