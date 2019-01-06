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
