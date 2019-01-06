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
