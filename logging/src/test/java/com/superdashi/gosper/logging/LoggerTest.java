package com.superdashi.gosper.logging;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.logging.LogDomain;
import com.superdashi.gosper.logging.Logger;

public class LoggerTest {

	@Test
	public void testStacktrace() throws InterruptedException {
		AtomicBoolean hasTrace = new AtomicBoolean();
		LogDomain domain = new LogDomain(e -> {
			hasTrace.set(e.stacktrace() != null && e.stacktrace().size() != 0);
		});
		domain.startProcessingLogEntries();
		Logger logger = domain.loggers().loggerFor("x", "y");
		logger.warning().stacktrace(new RuntimeException()).log();
		Thread.sleep(20);
		domain.stopProcessingLogEntries(0L);
		Assert.assertTrue(hasTrace.get());
	}

}
