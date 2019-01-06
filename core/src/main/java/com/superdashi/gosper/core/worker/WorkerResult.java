package com.superdashi.gosper.core.worker;

import java.util.Optional;

import com.tomgibara.fundament.Consumer;

public final class WorkerResult<T,C> {

	private final C context;
	private final T result;
	private final Exception exception;

	WorkerResult(C context, T result, Exception exception) {
		this.context = context;
		this.result = result;
		this.exception = exception;
	}

	public C getContext() {
		return context;
	}

	public Optional<T> getResult() {
		return Optional.ofNullable(result);
	}

	public Optional<Exception> getException() {
		return Optional.ofNullable(exception);
	}

}
