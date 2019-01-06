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