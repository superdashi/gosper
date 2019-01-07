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
