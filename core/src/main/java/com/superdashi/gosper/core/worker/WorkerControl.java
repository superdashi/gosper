package com.superdashi.gosper.core.worker;

import com.tomgibara.fundament.Consumer;

public class WorkerControl<C> {

	private final Worker<C> worker;

	public WorkerControl() {
		worker = new Worker<>();
	}

	public Worker<C> getWorker() {
		return worker;
	}

	public Consumer<C> collection() {
		return (context) -> worker.collect(context);
	}

	public void start() {
		worker.start();
	}

	public void collect(C context) {
		if (context == null) throw new IllegalArgumentException("null context");
		worker.collect(context);
	}

	public void stop(long timeout) {
		worker.stop(timeout);
	}
}
