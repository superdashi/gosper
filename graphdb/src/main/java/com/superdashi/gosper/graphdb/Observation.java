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

import java.util.ArrayList;
import java.util.List;

import com.tomgibara.fundament.Consumer;

//TODO could use a special lock object that prioritizes writes over reads for committed
public final class Observation {

	private final View view;
	// which parts are observed
	private final Selector selector;
	// who gets notified
	private final Consumer<PartRef> notifier;

	private Queue queue = new Queue();
	private final List<Queue> committed = new ArrayList<>();

	Observation(View view, Selector selector, Consumer<PartRef> notifier) {
		this.view = view;
		this.notifier = notifier;
		this.selector = selector;
		register();
	}

	public void deliver() {
		List<Queue> queues;
		synchronized (committed) {
			queues = new ArrayList<>(committed);
			committed.clear();
		}
		queues.stream().flatMap(q -> q.stream()).forEach(notifier::consume);
	}

	public boolean cancelled() {
		return view.space.registeredObservation(this);
	}

	public void cancel() {
		view.space.deregisterObservation(this);
	}

	void observe(Part part) {
		if (selector.matches(part)) queue.enqueue(part);
	}

	void commit() {
		synchronized (committed) {
			if (!queue.empty()) {
				queue.compact();
				committed.add(queue);
				queue = new Queue();
			}
		}
	}

	void rollback() {
		if (!queue.empty()) {
			queue = new Queue();
		}
	}

	private void register() {
		view.space.registerObservation(this);
	}

}
