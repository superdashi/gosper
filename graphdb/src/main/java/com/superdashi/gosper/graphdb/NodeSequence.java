package com.superdashi.gosper.graphdb;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

interface NodeSequence extends Iterable<Integer> {

	static final PrimitiveIterator.OfInt emptyIterator = new PrimitiveIterator.OfInt() {
		@Override public boolean hasNext() { return false; }
		@Override public int nextInt() { throw new NoSuchElementException(); }
	};

	static final NodeSequence empty = () -> emptyIterator;

	static PrimitiveIterator.OfInt singleIterator(int i) {
		return new PrimitiveIterator.OfInt() {
			private int next = i;
			@Override public boolean hasNext() { return next >= 0; }
			@Override public int nextInt() {
				if (next < 0) throw new NoSuchElementException();
				int ret = next;
				next = -1;
				return ret;
			}
		};
	}

	@Override
	PrimitiveIterator.OfInt iterator();

	default public IntStream stream() {
		Spliterator.OfInt spliterator = Spliterators.spliteratorUnknownSize(iterator(),
				Spliterator.DISTINCT  |
				Spliterator.IMMUTABLE |
				Spliterator.NONNULL   |
				Spliterator.ORDERED   |
				Spliterator.SORTED    );
		return StreamSupport.intStream(spliterator, false);
	}

	default public NodeSequence and(NodeSequence that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return () -> {
			return new PrimitiveIterator.OfInt() {
				private PrimitiveIterator.OfInt itA = iterator();
				private PrimitiveIterator.OfInt itB = that.iterator();
				private int next;

				{ advance(); }

				@Override
				public boolean hasNext() {
					return next >= 0;
				}

				@Override
				public int nextInt() {
					if (next < 0) throw new NoSuchElementException();
					int ret = next;
					advance();
					return ret;
				}

				private void advance() {
					next = -1;
					if (!itA.hasNext() || !itB.hasNext()) return;
					int a = itA.next();
					int b = itB.next();
					while (true) {
						int c = Integer.compare(a, b);
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
	default public NodeSequence or(NodeSequence that) {
		if (that == null) throw new IllegalArgumentException("null that");
		return () -> {
			return new PrimitiveIterator.OfInt() {
				private PrimitiveIterator.OfInt itA = iterator();
				private PrimitiveIterator.OfInt itB = that.iterator();
				private int nextA = -1;
				private int nextB = -1;

				{ advance(); }

				@Override
				public boolean hasNext() {
					return nextA >= 0 || nextB >= 0;
				}

				@Override
				public int nextInt() {
					if (nextA < 0 && nextB < 0) throw new NoSuchElementException();
					int ret;
					if (nextA < 0) {
						ret = nextB;
						nextB = -1;
					} else if (nextB < 0) {
						ret = nextA;
						nextA = -1;
					} else {
						int c = Integer.compare(nextA, nextB);
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
