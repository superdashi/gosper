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
