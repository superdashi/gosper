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
