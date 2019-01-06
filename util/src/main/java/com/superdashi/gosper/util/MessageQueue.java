package com.superdashi.gosper.util;

import java.util.ArrayList;
import java.util.Collection;

import com.tomgibara.fundament.Consumer;

public final class MessageQueue<M> {

	private final ArrayList<M> queue = new ArrayList<>();

	public synchronized void enqueue(M message) {
		queue.add(message);
	}

	public synchronized void enqueue(Collection<M> messages) {
		queue.addAll(messages);
	}

	@SuppressWarnings("unchecked")
	public void consumeMessages(Consumer<M> consumer) {
		Collection<M> messages;
		synchronized (queue) {
			messages = (Collection<M>) queue.clone();
			queue.clear();
		}
		messages.forEach(consumer::consume);
	}
}
