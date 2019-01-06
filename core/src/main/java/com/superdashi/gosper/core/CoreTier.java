package com.superdashi.gosper.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.superdashi.gosper.http.HttpServer;
import com.superdashi.gosper.http.HttpServer.ReqRes;
import com.tomgibara.fundament.Consumer;

public final class CoreTier {

	private final CoreContext context;
	private final HttpServer server;
	//TODO consider unifying with context executor
	private ScheduledExecutorService executor = null;

	public CoreTier(CoreContext context) {
		if (context == null) throw new IllegalArgumentException("null context");
		this.context = context;
		server = new HttpServer(context.serverPort());
	}

	public CoreContext context() {
		return context;
	}

	public void start() {
		checkRunning(false);
		executor = Executors.newSingleThreadScheduledExecutor();
		server.start(executor);
	}

	public void stop(long timeout) throws InterruptedException {
		checkRunning(true);
		server.stop();
		executor.shutdownNow();
		try {
			executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
		} finally {
			executor = null;
		}
	}

	public void setHandler(String uriPrefix, Consumer<ReqRes> handler) {
		server.setHandler(uriPrefix, handler);
	}

	public void clearHandler(String uriPrefix) {
		server.clearHandler(uriPrefix);
	}

	public ScheduledExecutorService executor() {
		//TODO need to create 'holding' executor to store runnables/callables until restarted
		if (executor == null) throw new UnsupportedOperationException();
		//TODO need to guard against 'control' of exposed executor
		return executor;
	}

	private boolean isRunning() {
		return executor != null;
	}

	private void checkRunning(boolean running) {
		if (isRunning()) {
			if (!running) throw new IllegalStateException("not running");
		} else {
			if (running) throw new IllegalStateException("running");
		}
	}

}
