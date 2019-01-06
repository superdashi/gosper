package com.superdashi.gosper.util;

import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.superdashi.gosper.util.Schedule;

public class ScheduleTest {

	@Test
	public void testBasic() throws InterruptedException {
		long duration = 50;
		long delay = 425;
		ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
		AtomicInteger count = new AtomicInteger();
		Schedule.at(duration, ChronoUnit.MILLIS).withDefaults().scheduling(() -> count.incrementAndGet()).on(ses);
		Thread.sleep(delay);
		int expected = (int) (delay / duration + 1);
		assertTrue((count.intValue() - expected) <= 1);
	}
}
