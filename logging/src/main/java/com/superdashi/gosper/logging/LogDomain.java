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
package com.superdashi.gosper.logging;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;

import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;
import com.tomgibara.collect.Collect.Sets;
import com.tomgibara.collect.EquivalenceSet;

public final class LogDomain {

	// static inner classes

	public interface Policy {
		boolean identityValid(LogIdentity identity);
		int initialQueueSize(LogIdentity identity);
		int maxQueueSize(LogIdentity identity);
		LogLevel logLevel(LogIdentity identity);
	}

	// statics

	private static Policy defaultPolicy = new Policy() {
		@Override public boolean identityValid(LogIdentity identity) { return true; }
		@Override public int initialQueueSize(LogIdentity identity) { return 8; }
		@Override public int maxQueueSize(LogIdentity identity) { return 256; }
		@Override public LogLevel logLevel(LogIdentity identity) { return LogLevel.INFO; }
	};

	private static final Maps<LogIdentity, LoggerRef> loggerMaps = Collect.setsOf(LogIdentity.class).mappedTo(LoggerRef.class);
	private static final Sets<LoggerRef> refSets = Collect.setsOf(LoggerRef.class).underIdentity();
	private static final Comparator<LogEntry> entryComparator = new Comparator<LogEntry>() {
		@Override
		public int compare(LogEntry a, LogEntry b) {
			if (a.timestamp == b.timestamp) {
				return Long.compare(a.id, b.id); //TODO ideally only if same identifier
			} else if (a.timestamp < b.timestamp) {
				return -1;
			} else {
				return 1;
			}
		}
	};

	// fields

	private final ReferenceQueue<Logger> queue = new ReferenceQueue<>();
	private final Map<LogIdentity, LoggerRef> refs = loggerMaps.newMap();
	private final EquivalenceSet<LoggerRef> work = refSets.newSet();
	private final Loggers loggers = new Loggers(this);
	private final LogRecorder recorder;
	private Policy policy = defaultPolicy;
	private boolean workFinishing = false;

	// constructors

	public LogDomain(LogRecorder recorder) {
		if (recorder == null) throw new IllegalArgumentException("null recorder");
		this.recorder = recorder;
	}

	// accessors

	public Loggers loggers() {
		return loggers;
	}

	public void policy(Policy policy) {
		if (policy == null) policy = defaultPolicy;
		this.policy = policy;
	}

	public Policy policy() {
		return policy == defaultPolicy ? null : policy;
	}

	// methods

	public void updateLoggerLevels() {
		synchronized (refs) {
			refs.values().forEach(ref -> {
				Logger logger = ref.get();
				if (logger != null) {
					logger.requiredLevel(policy.logLevel(ref.identity));
				}
			});
		}
	}

	public int activeLoggerEstimate() {
		synchronized (refs) {
			return refs.size();
		}
	}

	public boolean stopProcessingLogEntries(long timeout) throws InterruptedException {
		synchronized (work) {
			workFinishing = true;
			work.notifyAll();
			if (timeout > 0L) {
				final long target = System.currentTimeMillis() + timeout;
				while (workFinishing && System.currentTimeMillis() < target) {
					work.wait(timeout);
				}
			}
			return !workFinishing;
		}
	}

	// convenience methods that calls processLogEntries on a newly created dedicated thread
	public void startProcessingLogEntries() {
		checkWorking();
		new Thread(() -> {
			try {
				processLogEntries();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	// call instead of startProcessingLogEntries for more control over threading
	public void processLogEntries() throws InterruptedException {
		checkWorking();
		PriorityQueue<LogEntry> entries = new PriorityQueue<>(entryComparator);
		while (true) {
			synchronized (work) {
				while (work.isEmpty() && !workFinishing) {
					work.wait();
				}
				if (work.isEmpty() && workFinishing) { // only honour work-over once we've flushed all accumulated work
					workFinishing = false;
					work.notifyAll(); // we notify here to communicate finish to stopper
					return;
				}
				work.forEach(ref -> ref.logQueue.removeAll(entries));
				work.clear(); // we promise to do all the work
			}
			while (true) {
				LogEntry entry = entries.poll();
				if (entry == null) break;
				try {
					recorder.record(entry);
				} catch (RuntimeException | IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// package scoped methods

	Logger loggerFor(LogIdentity identity) {
		if (!policy.identityValid(identity)) throw new IllegalArgumentException("invalid identity");
		synchronized (refs) {
			reapLoggers();
			LoggerRef ref = refs.get(identity);
			Logger logger = ref == null ? null : ref.get();
			if (logger == null) {
				Notifier notifier = new Notifier();
				//TODO guard against bad policy values
				LogQueue logQueue = new LogQueue(notifier, policy.initialQueueSize(identity), policy.maxQueueSize(identity));
				logger = new Logger(this, identity, logQueue, policy.logLevel(identity));
				notifier.ref = ref = new LoggerRef(logger, queue, logQueue);
				refs.put(identity, ref);
			}
			return logger;
		}
	}

	// private helper methods

	private void checkWorking() {
		synchronized (work) {
			if (workFinishing) throw new IllegalStateException("log entry processing already stopped");
		}
	}

	// must be called with lock
	private void reapLoggers() {
		while (true) {
			LoggerRef ref = (LoggerRef) queue.poll();
			if (ref == null) break;
			refs.remove(ref.identity, ref);
		}
	}

	// inner classes

	class Notifier implements Runnable {

		LoggerRef ref;

		@Override
		public void run() {
			synchronized (work) {
				if (workFinishing) return;
				work.add(ref);
				work.notify();
			}
		}
	}
}
