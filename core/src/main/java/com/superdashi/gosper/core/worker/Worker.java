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
package com.superdashi.gosper.core.worker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.superdashi.gosper.core.DashiLog;
import com.tomgibara.fundament.Consumer;

//TODO add logging
//TODO consider adding priorities
//TODO add defer methods that take only resultHandler?
public class Worker<C> {

	private static <T> Callable<T> callable(Runnable r) {
		return () -> { r.run(); return null; };
	}

	private ScheduledExecutorService executor = null;
	private final Deque<Job<?>> postQueue = new ArrayDeque<>(); //synced on self, not lock

	Worker() { }

	void start() {
		if (executor != null) throw new IllegalStateException("worker already started");
		DashiLog.debug("worker starting");
		executor = Executors.newSingleThreadScheduledExecutor();
		DashiLog.debug("worker started");
	}

	// handlers executed during collection
	public <T> void call(Callable<T> task, Consumer<WorkerResult<T, C>> resultHandler) {
		if (task == null) throw new IllegalArgumentException("null task");
		executor.submit(new Job<>(task, resultHandler));
	}

	public <T> void run(Runnable task, Consumer<WorkerResult<Void, C>> resultHandler) {
		if (task == null) throw new IllegalArgumentException("null task");
		call(callable(task), resultHandler);
	}

	public <T> void schedule(Callable<T> task, Consumer<WorkerResult<T, C>> resultHandler, long delayInMillis) {
		if (task == null) throw new IllegalArgumentException("null task");
		executor.schedule(new Job<>(task, resultHandler), delayInMillis, TimeUnit.MILLISECONDS);
	}

	public <T> void schedule(Runnable task, Consumer<WorkerResult<T, C>> resultHandler, long delayInMillis) {
		if (task == null) throw new IllegalArgumentException("null task");
		schedule(callable(task), resultHandler, delayInMillis);
	}

	public <T> void enqueue(T result, Consumer<WorkerResult<T, C>> resultHandler) {
		if (resultHandler == null) throw new IllegalArgumentException("null resultHandler");
		synchronized (postQueue) {
			postQueue.add(new Job<>(result, resultHandler));
		}
	}

	void collect(C context) {
		while (true) {
			Job<?> job;
			synchronized (postQueue) {
				job = postQueue.poll();
				if (job == null) break;
			}
			job.doPost(context);
		}
	}

	void stop(long timeout) {
		if (executor == null) throw new IllegalStateException("worker not started");
		DashiLog.debug("worker stopping");
		List<Runnable> remaining = executor.shutdownNow();
		//TODO what to do with remaining jobs and with lingering post work
		DashiLog.debug("worker stopped");
		executor = null;
	}

	private class Job<T> implements Runnable {

		private final Callable<T> task;
		private final Consumer<WorkerResult<T, C>> resultHandler;

		private T result = null;
		private Exception exception = null;

		Job(Callable<T> task, Consumer<WorkerResult<T, C>> resultHandler) {
			this.task = task;
			this.resultHandler = resultHandler;
		}

		// shortcut - create a job that's already 'run' successfully
		Job(T result, Consumer<WorkerResult<T, C>> resultHandler) {
			this.task = null;
			this.resultHandler = resultHandler;
			this.result = result;
		}

		@Override
		public void run() {
			try {
				result = task.call();
			} catch (Exception e) {
				exception = e;
			} finally {
				// queue it for collection
				synchronized (postQueue) {
					postQueue.add(this);
				}
			}
		}

		void doPost(C context) {
			if (resultHandler != null) {
				resultHandler.consume(new WorkerResult<>(context, result, exception));
			} else if (exception != null) {
				DashiLog.warn("untrapped exception on worker", exception);
			}
		}
	}
}
