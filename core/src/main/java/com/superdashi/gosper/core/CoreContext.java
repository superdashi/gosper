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
package com.superdashi.gosper.core;

import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;

import com.superdashi.gosper.logging.Loggers;

public interface CoreContext {

	public static CoreContext system(String title, CacheControl cacheCtrl, Runnable shutdown, ScheduledExecutorService backgroundExecutor, Loggers loggers) {
		return new CoreContext() {
			@Override public Loggers loggers() { return loggers; }
			@Override public ZoneId zoneId() { return ZoneId.systemDefault(); }
			@Override public Clock clock() { return Clock.systemUTC(); }
			@Override public Cache cache() { return cacheCtrl.getCache(); }
			@Override public int serverPort() { return 8000; }
			@Override public String title() { return title; }
			@Override public Runnable shutdown() { return shutdown; }
			@Override public ScheduledExecutorService backgroundExecutor() { return backgroundExecutor; }
		};
	}

	Loggers loggers();

	ZoneId zoneId();

	Clock clock();

	Cache cache();

	String title();

	ScheduledExecutorService backgroundExecutor();

	Runnable shutdown();

	int serverPort();

}
