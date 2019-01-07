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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DoneFuture<T> implements Future<T> {

	private final T value;

	public DoneFuture(T value) {
		this.value = value;
	}

	@Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
	@Override public boolean isCancelled() { return false; }
	@Override public boolean isDone() { return true; }
	@Override public T get() { return value; }
	@Override public T get(long timeout, TimeUnit unit) { return value; }

}
