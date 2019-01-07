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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

final class LoggerRef extends PhantomReference<Logger> {

	final LogIdentity identity;
	final LogQueue logQueue;

	LoggerRef(Logger logger, ReferenceQueue<? super Logger> queue, LogQueue logQueue) {
		super(logger, queue);
		identity = logger.identity();
		this.logQueue = logQueue;
	}

}