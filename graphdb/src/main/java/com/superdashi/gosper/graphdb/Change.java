package com.superdashi.gosper.graphdb;

import java.util.List;

interface Change {

	default void record(List<Change> changes) {
		changes.add(this);
	}

	void applyTo(Indices indices);
}
