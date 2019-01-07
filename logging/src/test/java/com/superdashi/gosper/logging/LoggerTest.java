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
