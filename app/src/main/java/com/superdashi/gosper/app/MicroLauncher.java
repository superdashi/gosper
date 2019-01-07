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

import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Set;

import com.superdashi.gosper.awtd.AWTDevice;
import com.superdashi.gosper.awtd.AWTDeviceChooser;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.http.HttpReqRes;
import com.superdashi.gosper.micro.Interface;
import com.superdashi.gosper.micro.Interfaces;
import com.superdashi.gosper.util.DashiUtil;
import com.superdashi.gosper.util.Duration;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;

public class MicroLauncher {

	//TODO tidy up this mess
	private static final long SHUTDOWN_TIMEOUT = 1000L;

	//TODO make configurable via args?
	private final static long STOP_TIMEOUT = 2000L;
	//TODO make configurable in config
	private final static Duration pollDelay = new Duration(5000L);

	// "arm" reported by Oracle, but "aarch32" reported by Azul
	// JOGL's platform sniffing doesn't like this, so we kludge it
	private static void kludgeOSArch() {
		String osArch = System.getProperty("os.arch");
		if ("aarch32".equals(osArch)) {
			String altOsArch = "arm";
			System.setProperty("os.arch", altOsArch);
			DashiLog.warn("Tweaked reported os.arch from {0} to {1}", osArch, altOsArch);
		}
	}

	private static final String DEFAULT_CONFIG_PATH = "/etc/opt/gosper/";

	private static final Shutdown shutdown = new Shutdown();

	public static void main(String... args) throws InterruptedException {
		kludgeOSArch();

		// identify the configuration path
		String pathStr;
		if (args.length == 0) {
			pathStr = DEFAULT_CONFIG_PATH;
		} else {
			pathStr = args[0];
		}

		// create the configuration
		Configuration configuration;
		try {
			Path path = Paths.get(pathStr);
			configuration = new Configuration(path, pollDelay, shutdown::trigger);
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid parameters: " + e.getMessage());
			System.exit(1);
			// not possible
			configuration = null;
		}

		if (!configuration.valid()) {
			System.err.println("Configuration not valid");
			try {
				configuration.destroy();
			} finally {
				System.exit(1);
			}
		}

		try {
			// launch - note: blocks waiting for shutdown
			new MicroLauncher(configuration);
		} finally {

			// stop the configuration
			try {
				configuration.stop(STOP_TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			configuration.destroy();
		}
	}

	private final Tiers tiers;

	private MicroLauncher(Configuration configuration) throws InterruptedException {
		assert configuration != null && configuration.valid();

		// create the tiers
		tiers = configuration.start();

		// add fallback interface
		Interfaces interfaces;
		if (tiers.microTier == null) {
			interfaces = null;
		} else {
			interfaces = tiers.microTier.interfaces();
			if (interfaces.toCollection().isEmpty()) {
				if (GraphicsEnvironment.isHeadless()) {
					//TODO this should be relaxed
					// don't start in the absence of a device
					System.err.println("No device configured");
					System.exit(1);
				}
				// launch chooser in the absence of devices
				AWTDevice device = AWTDeviceChooser.choose(new TestWifi(new Random()));
				if (device == null) System.exit(0);
				interfaces.addInterface("chosen", device, configuration::dataContext);
			}
		}

		// start
		tiers.coreTier.setHandler("/control/", this::control);
		tiers.start();

		// launch apps
		if (interfaces != null) launchApps(interfaces, configuration.installedApps.appURIs());

		// wait for shutdown
		shutdown.await();

		DashiLog.info("shutting down");
		tiers.stop(SHUTDOWN_TIMEOUT);
		tiers.dataTier.destroy();
		DashiLog.debug("shutdown completed");

	}

	private void launchApps(Interfaces faces, Set<URI> apps) throws InterruptedException {
		//faces.toCollection().stream().forEach(i -> apps.forEach(a -> i.instantiate(a)));
		for (Interface face : faces.toCollection()) {
			for (URI appURI : apps) {
				face.instantiate(appURI);
			}
			face.launchDefault();
		}
	}

	private void control(HttpReqRes reqres) {
		String method = reqres.request().method();
		String path = reqres.request().path();
		if (!path.endsWith("/")) path = path + "/";
		if (method.equals("GET") && path.equals("/")) {
			reqres.respondText(400, "text/html", controlHtml() + " " + reqres.request().path());
		} else if (method.equals("POST") && path.startsWith("/stop/")) {
			reqres.respondText(400, "text/plain", "Shutting down");
			shutdown.trigger();
		}
	}

	private String controlHtml() {
		try (ReadStream in = Streams.streamInput(MicroLauncher.class.getResourceAsStream("/control.html"))) {
			StreamBytes bytes = Streams.bytes();
			bytes.writeStream().from(in).transferFully();
			return new String(bytes.bytes(), DashiUtil.UTF8);
		}
	}

	private static class Shutdown {

		private boolean shutdown = false;

		private synchronized void trigger() {
			DashiLog.debug("shutdown triggered");
			shutdown = true;
			notifyAll();
		}

		private synchronized void await() {
			try {
				while (!shutdown) wait();
				DashiLog.debug("shutdown observed");
			} catch (InterruptedException e) {
				DashiLog.warn("interrupted before shutdown request");
			}
		}

	}
}
