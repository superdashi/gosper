package com.superdashi.gosper.util;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


//TODO add trace logging to this
public final class Scheduling {

	private final Scheduler schedule;
	private final Runnable runnable;

	Scheduling(Scheduler schedule, Runnable runnable) {
		this.schedule = schedule;
		this.runnable = runnable;
	}

	public Future<?> on(ScheduledExecutorService ses) {
		return new Repeater(ses);
	}

	public Poller poller() {
		return new Poller();
	}

	public final class Poller {

		private Instant next;

		Poller() {
			next = schedule.first();
		}

		boolean poll() {
			Instant now = schedule.clock().instant();
			if (now.isBefore(next)) return false;
			next = schedule.nextAfter(now);
			runnable.run();
			return true;
		}
	}

	private final class Repeater implements Runnable, Future<Void> {

		private final ScheduledExecutorService ses;
		private Instant last = null;
		private boolean catchUp;
		private Future<?> future;

		Repeater(ScheduledExecutorService ses) {
			this.ses = ses;
			schedule();
		}

		@Override
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			return future.cancel(mayInterruptIfRunning);
		}

		@Override
		public Void get() throws InterruptedException, ExecutionException {
			//TODO what is the correct implementation here?
			return null;
		}

		@Override
		public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			//TODO what is the correct implementation here?
			return null;
		}

		@Override
		public synchronized boolean isCancelled() {
			return future.isCancelled();
		}

		@Override
		public synchronized boolean isDone() {
			return future.isDone();
		}

		@Override
		public void run() {
			try {
				runnable.run();
			} finally {
				try {
					schedule();
				} catch (RuntimeException e) {
					//TODO how to deal with this
					e.printStackTrace();
				}
			}
		}

		private synchronized void schedule() {
			Instant now = schedule.clock().instant();
			Instant next = last == null ? schedule.first() : schedule.nextAfter(catchUp ? now : last);

			catchUp = next.isBefore(now);
			last = next;

			if (catchUp) {
				future = ses.submit(this);
			} else {
				long delay = next.toEpochMilli() - now.toEpochMilli();
				try {
					future = ses.schedule(this, delay, TimeUnit.MILLISECONDS);
				} catch (RejectedExecutionException e) {
					//TODO want to log this
				}
			}
		}
	}
}
