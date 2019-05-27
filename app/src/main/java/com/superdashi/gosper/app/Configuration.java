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
package com.superdashi.gosper.app;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import com.superdashi.gosper.logging.LogDomain;
import com.superdashi.gosper.logging.LogPolicyFile;
import com.superdashi.gosper.logging.LogRecorder;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.logging.Loggers;
import com.superdashi.gosper.micro.MicroTier;
import com.superdashi.gosper.micro.Visuals;
import com.superdashi.gosper.scripting.ScriptEngine;
import com.superdashi.gosper.util.Duration;

final class Configuration {

	private static final String PROPERTY_SCRIPT_ENGINE = "com.superdashi.gosper.micro.SCRIPT_ENGINE";

	//TODO need a sensible way to handle these timeout constants
	private static final long LOG_STOP_TIMEOUT = 1000L;

	private static final Path pathLogPolicy = Paths.get("logging.policy");
	private static final Path pathCoreConfig = Paths.get("core.config");
	private static final Path pathInstalledApps = Paths.get("installed-apps");
	private static final Path pathVisuals = Paths.get("visuals");
	private static final Path pathDevices = Paths.get("devices");

	// config
	final Path path;
	private final Duration pollDelay;

	// fields used for active configuration
	private final WatchService watcher;
	private final WatchKey watchKey;
	private final Thread watchThread;

	// elements of configuration
	final LogDomain logDomain;
	private final LogPolicyFile logPolicy;
	private final CoreConfig coreConfig;
	final InstalledApps installedApps;
	final Visuals visuals;
	final Devices devices;
	final ScriptEngine scriptEngine;

	// state
	private final Logger logger;
	private Tiers tiers = null;

	Configuration(Path path, Duration pollDelay, Runnable shutdown) {
		if (path == null) throw new IllegalArgumentException("null path");
		if (!Files.isDirectory(path)) throw new IllegalArgumentException("path not a directory");
		if (pollDelay == null) throw new IllegalArgumentException("null pollDelay");
		if (shutdown == null) throw new IllegalArgumentException("null shutdown");
		this.path = path;
		this.pollDelay = pollDelay;

		// watching config directory if possible
		WatchService watcher;
		WatchKey watchKey;
		Thread watchThread;
		try {
			watcher = FileSystems.getDefault().newWatchService();
			watchKey = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			watchThread = new Thread(this::watch);
		} catch (IOException e) {
			/* won't watch */
			watcher = null;
			watchKey = null;
			watchThread = null;
		}
		this.watcher = watcher;
		this.watchKey = watchKey;
		this.watchThread = watchThread;

		// configure logging and start it, want logging up asap
		//TODO make recorder configurable
		logDomain = new LogDomain(LogRecorder.sys());
		logPolicy = new LogPolicyFile(path.resolve(pathLogPolicy));
		logDomain.policy(logPolicy);
		logDomain.startProcessingLogEntries();
		Loggers loggers = logDomain.loggers();
		logger = loggers.loggerFor("configuration");

		// configure visuals
		visuals = new Visuals(loggers.loggerFor("visual"), path.resolve(pathVisuals));

		// configure installed apps
		installedApps = new InstalledApps(loggers.loggerFor("app-inst"), path.resolve(pathInstalledApps));

		// identify installed devices
		devices = new Devices(loggers, path.resolve(pathDevices));

		// instantiate script engine (if specified)
		scriptEngine = createScriptEngine();
		if (scriptEngine != null) {
			logger.info().message("script engine defined {}").values(scriptEngine.getClass().getName()).log();
		}

		// create core context
		//TODO could make context concrete?
		coreConfig = CoreConfig.parseCoreConfig(logger, path.resolve(pathCoreConfig), shutdown, loggers);

	}

	boolean valid() {
		return coreConfig != null;
	}

	Tiers start() {
		// create the tiers
		if (tiers != null) return tiers;
		if (!valid()) throw new IllegalStateException("not valid");
		tiers = coreConfig.newTiersBuilder().visuals(visuals).scriptEngine(scriptEngine).build();
		MicroTier microTier = tiers.microTier;
		if (microTier != null) {
			installedApps.adjustApps(microTier.appInstalls(), tiers.dataTier);
			devices.addInterfaces(microTier.interfaces(), tiers.dataTier);
		}

		// start the watching, or fallback to polling
		if (watchThread == null) {
			long pollDelayTime = pollDelay.getTime();
			coreConfig.backgroundExecutor().scheduleWithFixedDelay( this::poll, pollDelayTime, pollDelayTime, TimeUnit.MILLISECONDS);
		} else {
			watchThread.start();
		}

		return tiers;
	}

	void stop(long timeout) throws InterruptedException {
		if (timeout < 0L) throw new IllegalArgumentException("negative timeout");
		if (tiers == null) return;
		try {
			// stop watching
			if (watchThread != null) {
				watchThread.interrupt();
				watchThread.join(timeout);
			}
			if (watchKey != null) {
				watchKey.cancel();
			}
		} finally {
			tiers = null;
		}
	}

	void destroy() throws InterruptedException {
		// stop configured elements
		logDomain.stopProcessingLogEntries(LOG_STOP_TIMEOUT);
		if (coreConfig != null) {
			coreConfig.backgroundExecutor().shutdown();
		}
	}

	private void poll() {
		{
			boolean changed = logPolicy.poll();
			if (changed) logDomain.updateLoggerLevels();
		}
	}

	private void watch() {
		while (true) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException e) {
				/* we're done, just leave */
				return;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				Path path = (Path) event.context();
				if (path.equals(pathLogPolicy)) {
					boolean changed = logPolicy.update();
					if (changed) logDomain.updateLoggerLevels();
				} else if (path.equals(pathCoreConfig)) {
					logger.warning().message("core configuration changed, restart required for changes to take effect").filePath(pathCoreConfig).log();
				}else if (path.equals(pathInstalledApps)) {
					boolean changed = installedApps.update();
					if (changed) installedApps.adjustApps(tiers.microTier.appInstalls(), tiers.dataTier);
				}
			}
			key.reset();
		}
	}

	private ScriptEngine createScriptEngine() {
		ScriptEngine scriptEngine;
		String className = System.getProperty(PROPERTY_SCRIPT_ENGINE);
		if (className == null || className.isEmpty()) {
			logger.debug().message("no script engine defined via {}").values(PROPERTY_SCRIPT_ENGINE).log();
			scriptEngine = null;
		} else {
			Object instance = null;
			try {
				Class<?> clss = Configuration.class.getClassLoader().loadClass(className);
				instance = clss.newInstance();
			} catch (ClassNotFoundException e) {
				logger.error().message("invalid scripting class: {}").values(PROPERTY_SCRIPT_ENGINE).stacktrace(e).log();
			} catch (InstantiationException e) {
				logger.error().message("failed to instantiate scripting class: {}").values(className).stacktrace(e).log();
			} catch (IllegalAccessException e) {
				logger.error().message("failed to access scripting class: {}").values(className).stacktrace(e).log();
			}
			try {
				scriptEngine = (ScriptEngine) instance;
			} catch (ClassCastException e) {
				logger.error().message("scripting class {} not a {}").values(PROPERTY_SCRIPT_ENGINE, ScriptEngine.class.getName()).stacktrace(e).log();
				scriptEngine = null;
			}
		}
		return scriptEngine;
	}

}