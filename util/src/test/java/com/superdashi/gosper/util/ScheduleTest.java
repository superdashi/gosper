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
