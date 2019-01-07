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

import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Sets;
import com.tomgibara.collect.EquivalenceSet;

final class Observer {

	private static final Sets<Observation> sets = Collect.setsOf(Observation.class).underIdentity();

	private final EquivalenceSet<Observation> set;

	Observer() {
		this.set = sets.emptySet();
	}

	private Observer(EquivalenceSet<Observation> set) {
		this.set = set;
	}

	void observe(Part part) {
		set.forEach(obs -> obs.observe(part));
	}

	void commit() {
		set.forEach(Observation::commit);
	}

	void rollback() {
		set.forEach(Observation::rollback);
	}

	Observer with(Observation obs) {
		if (set.contains(obs)) return this;
		EquivalenceSet<Observation> newSet = set.mutableCopy();
		newSet.add(obs);
		return new Observer(newSet.immutableView());
	}

	Observer without(Observation obs) {
		if (!set.contains(obs)) return this;
		EquivalenceSet<Observation> newSet = set.mutableCopy();
		newSet.remove(obs);
		return new Observer(newSet.immutableView());
	}

	boolean observes(Observation obs) {
		return set.contains(obs);
	}

}
