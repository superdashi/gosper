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
