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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.logging.LogAppender;
import com.superdashi.gosper.logging.LogDomain;
import com.superdashi.gosper.logging.LogEntry;
import com.superdashi.gosper.logging.LogFormatter;
import com.superdashi.gosper.logging.LogRecorder;
import com.superdashi.gosper.logging.Logger;

public class LogDomainTest {

	private static final LogFormatter formatter = new LogFormatter() {
		@Override
		public <T> LogAppender<T> format(LogAppender<T> appender, LogEntry entry) throws IOException {
			return appender
				.append(entry.message)
				.append(' ')
				.append(Long.toString(entry.timestamp))
				.append(" (")
				.append(Long.toString(System.currentTimeMillis() - entry.timestamp))
				.append(')')
				.appendNewline();
		}
	};

	@Test
	public void testReaping() throws InterruptedException {
		LogDomain domain = new LogDomain(LogRecorder.devnull());
		String type = "SRC";
		int count = 100000;
		//long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			domain.loggers().loggerFor(type, Integer.toString(i));
		}
		//long finish = System.currentTimeMillis();
		//System.out.println(finish - start);
		Thread.sleep(50);
		Assert.assertTrue(domain.activeLoggerEstimate() < count);
	}

	@Test
	public void testStopping() throws InterruptedException {
		LogDomain domain = new LogDomain(LogRecorder.devnull());
		AtomicBoolean running = new AtomicBoolean();
		new Thread(() -> {
			try {
				running.set(true);
				domain.processLogEntries();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				running.set(false);
			}
		}).start();
		//TODO hacky
		Thread.sleep(10);
		assertTrue(running.get());
		domain.stopProcessingLogEntries(0L);
		Thread.sleep(10);
		assertFalse(running.get());
	}

	@Test
	public void testLogging() throws InterruptedException {
		AtomicBoolean flag = new AtomicBoolean(true);
		LogDomain domain = new LogDomain(e -> flag.set(true));
		//LogDomain domain = new LogDomain(LogRecorder.stdoutRecorder(formatter));
		new Thread(() -> {
			flag.set(false);
			try {
				domain.processLogEntries();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
		while (flag.get());
		Logger logger = domain.loggers().loggerFor("TYP", "src");
		for (int i = 0; i < 100; i++) {
			logger.error().message("Oops! {}").values(i).log();
		}
		Thread.sleep(20);
		assertTrue(flag.get());
	}

	@Test
	public void testOverflow() throws InterruptedException {
		LogDomain domain = new LogDomain(e -> {
			try {
				Thread.sleep(1);
			} catch (InterruptedException ex) { }
		});
		new Thread(() -> {
			try {
				domain.processLogEntries();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
		Logger logger = domain.loggers().loggerFor("TYP", "src");
		boolean overflowed = false;
		for (int i = 0; i < 10000 && !overflowed; i++) {
			overflowed = !logger.error().message("Oops! {}").values(i).log();
		}
		assertTrue(overflowed);
	}
}
