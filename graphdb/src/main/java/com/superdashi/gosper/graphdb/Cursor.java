package com.superdashi.gosper.graphdb;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Cursor<T> extends Iterable<T> {

	// origin

	Visit visit();

	// streams

	Stream<T> stream();

	// iterators

	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	// lists

	default List<T> toList() {
		return stream().collect(Collectors.toList());
	}

	// sets

	default Set<T> toSet() {
		return stream().collect(Collectors.toSet());
	}

	// others

	default Optional<T> possibleUnique() {
		Iterator<T> it = iterator();
		if (!it.hasNext()) return Optional.empty();
		T t = it.next();
		return it.hasNext() ? Optional.empty() : Optional.of(t);
	}

	default T unique() {
		Iterator<T> it = iterator();
		if (!it.hasNext()) throw new ConstraintException(ConstraintException.Type.NON_EXISTENT);
		T t = it.next();
		if (it.hasNext()) throw new ConstraintException(ConstraintException.Type.NON_UNIQUE);
		return t;
	}

	default Optional<T> first() {
		return stream().findFirst();
	}

	default int count() {
		return (int) stream().count();
	}

	default boolean empty() {
		return count() == 0;
	}
}
