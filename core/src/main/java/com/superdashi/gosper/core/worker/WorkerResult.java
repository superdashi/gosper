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
