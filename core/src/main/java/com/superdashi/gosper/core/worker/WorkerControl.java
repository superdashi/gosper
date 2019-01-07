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
