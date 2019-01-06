package com.superdashi.gosper.graphdb;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO consider making abstract base class
public interface PartCursor<P extends Part> extends Iterable<P> {

	// origin

	Visit visit();

	// order

	Order order();

	PartCursor<P> order(Order order);

	// streams

	Stream<P> stream();

	// iterators

	@Override
	default Iterator<P> iterator() {
		return stream().iterator();
	}

	// lists

	default List<P> toList() {
		return stream().collect(Collectors.toList());
	}

	// sets

	default Set<P> toSet() {
		return stream().collect(Collectors.toSet());
	}

	// others

	default Optional<P> possibleUnique() {
		Iterator<P> it = iterator();
		if (!it.hasNext()) return Optional.empty();
		P p = it.next();
		return it.hasNext() ? Optional.empty() : Optional.of(p);
	}

	default P unique() {
		Iterator<P> it = iterator();
		if (!it.hasNext()) throw new ConstraintException(ConstraintException.Type.NON_EXISTENT_PART);
		P p = it.next();
		if (it.hasNext()) throw new ConstraintException(ConstraintException.Type.NON_UNIQUE_PART);
		return p;
	}

	default Optional<P> first() {
		return stream().findFirst();
	}

	default int count() {
		return (int) stream().count();
	}

	default boolean empty() {
		return count() == 0;
	}
}
