package com.superdashi.gosper.graphdb;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

interface TypeSequence extends Iterable<Long> {

	// TODO consider unifying with EdgeSequence.emptyIterator
	static final PrimitiveIterator.OfLong emptyIterator = new PrimitiveIterator.OfLong() {
		@Override public boolean hasNext() { return false; }
		@Override public long nextLong() { throw new NoSuchElementException(); }
	};

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

}
