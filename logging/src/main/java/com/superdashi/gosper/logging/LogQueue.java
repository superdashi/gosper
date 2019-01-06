package com.superdashi.gosper.logging;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

class LogQueue {

	// fields

	private final Runnable notifier;
	private final int maxSize;
	private LogEntry[] entries;
	// equality of tail and head indicates either full or empty
	private int head = 0; // index of oldest entry
	private int tail = 0; // index where next entry is to be added
	private long count = 0L; // total number of entries added to this queue

	// constructors

	LogQueue(Runnable notifier, int initialSize, int maxSize) {
		this.notifier = notifier;
		this.maxSize = maxSize;
		entries = new LogEntry[initialSize];
	}

	// accessors

	int capacity() {
		synchronized (this) {
			return entries.length;
		}
	}

	int size() {
		synchronized (this) {
			if (head == tail) return entries[head] == null ? 0 : entries.length;
			if (head < tail) return tail - head;
			return entries.length + tail - head;
		}
	}

	int maxSize() {
		return maxSize;
	}

	long count() {
		return count;
	}

	// queue access

	// returns false if overflowed
	boolean add(LogEntry entry) {
		if (entry == null) throw new IllegalArgumentException("null entry");
		boolean wasEmpty;
		synchronized (this) {
			count++;
			if (tail == head && entries[tail] != null) { // we're full
				int capacity = Math.min(entries.length * 2, maxSize);
				if (capacity == maxSize) return false; // overflow
				LogEntry[] newEntries = new LogEntry[capacity];
				System.arraycopy(entries, head, newEntries, 0, entries.length - head);
				System.arraycopy(entries, 0, newEntries, entries.length - head, head);
				head = 0;
				tail = entries.length;
				entries = newEntries;
			}
			entries[tail] = entry;
			wasEmpty = head == tail;
			tail = advance(tail);
		}
		// notification made outside of sync block to remove possibility of deadlocks
		if (wasEmpty) {
			notifier.run();
		}
		return true;
	}

	Optional<LogEntry> remove() {
		LogEntry entry;
		synchronized (this) {
			entry = entries[head];
			if (entry == null) return Optional.empty(); // underflow
			entries[head] = null;
			head = advance(head);
		}
		return Optional.of(entry);
	}

	// note, does not guarantee order
	void removeAll(Collection<LogEntry> target) {
		synchronized (this) {
			if (head == tail) {
				if (entries[head] == null) return; // empty case - do nothing
				target.addAll(Arrays.asList(entries)); // full case - add everything
			} else {
				List<LogEntry> list = Arrays.asList(entries);
				if (head < tail) { // unified slice case
					target.addAll(list.subList(head, tail));
				} else { // split slice case
					target.addAll(list.subList(head, list.size()));
					target.addAll(list.subList(0, tail));
				}
			}
			head = tail;
			Arrays.fill(entries, null);
		}
	}

	private int advance(int index) {
		return ++index == entries.length ? 0 : index;
	}

}
