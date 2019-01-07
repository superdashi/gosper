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
package com.superdashi.gosper.micro;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import com.superdashi.gosper.bundle.AppRole;
import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.bundle.BundleCollation;
import com.superdashi.gosper.data.DataContext;
import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.scripting.ScriptSession;
import com.superdashi.gosper.studio.Studio;
import com.superdashi.gosper.studio.SurfacePool;

public final class Interface {

	// statics

	// fields

	public final Runtime runtime;
	public final String identifier;
	private final AppInstalls appInstalls;
	final Device device;
	final Function<Identity, DataContext> dbConnector; // optional
	final Studio studio;
	final SurfacePool surfacePool;
	final Logger logger;
	final ScriptSession scriptSession;

	final AppInstance baseAppInstance;
	private boolean attached;
	private boolean deviceCaptured;
	private Screen screen;
	private Qualifier qualifier;
	private Directory directory;
	//TODO would prefer one manager over all interfaces
	private ActivityManager manager;

	// constructors

	Interface(Runtime runtime, String identifier, Device device, Function<Identity, DataContext> dbConnector, Studio studio) {
		this.runtime = runtime;
		appInstalls = runtime.appInstalls;
		this.identifier = identifier;
		this.device = device;
		this.dbConnector = dbConnector;
		this.studio = studio;
		surfacePool = studio.createSurfacePool();
		logger = runtime.logger.descendant("interfaces", identifier);
		baseAppInstance = instantiate(appInstalls.baseBundle);
		scriptSession = runtime.scriptEngine == null ? null : runtime.scriptEngine.openSession(identifier);
	}

	// public methods

	// empty if app duplicates an existing app
	public Optional<String> instantiate(URI appUri) {
		synchronized (appInstalls.lifetime) {
			checkAttached();
			// obtain the app data from the runtime
			Bundle appData = appInstalls.bundleForUri(appUri);
			if (appData == null) throw new IllegalArgumentException("no application with URI " + appUri);
			// instantiate the application
			AppInstance instance = instantiate(appData);
			// register it in the directory
			boolean registered = directory.registerApp(instance);
			// return the instance id
			return registered ? Optional.of(instance.instanceId) : Optional.empty();
		}
	}

	public void launchInstance(String instanceId) throws InterruptedException {
		synchronized (appInstalls.lifetime) {
			checkAttached();
			Environment env = directory.envForInstanceId(instanceId);
			manager.launchApplication(env.appInstance);
		}
	}

	public void launchDefault() throws InterruptedException {
		synchronized (appInstalls.lifetime) {
			checkAttached();
			Optional<Bundle> opt = appInstalls.bundleForRole(AppRole.LAUNCH);
			if (!opt.isPresent()) opt = appInstalls.bundleForRole(AppRole.PASSIVE);
			if (opt.isPresent()) {
				String instanceId = opt.get().instanceId;
				Environment env = directory.envForInstanceId(instanceId);
				AppInstance instance = env.appInstance;
				manager.launchApplication(instance);
			}
		}
	}

	// package scoped methods

	void attach() throws InterruptedException {
		checkNotAttached();
		attached = true;

		// initialize device
		if (!deviceCaptured) {
			device.capture();
			deviceCaptured = true;
		}

		// obtain screen
		screen = device.getScreen().get();
		screen.begin();

		// get a reference to the device spec for convenience
		DeviceSpec deviceSpec = device.getSpec();

		// establish the initial qualifying state
		qualifier = Qualifier.with(Locale.getDefault(), deviceSpec.screenClass, deviceSpec.screenColor, Flavor.GENERIC);

		// create a directory to store the applications
		directory = new Directory(this);

		// create manager
		manager = new ActivityManager(this);

		// notify script engine (if any)
		if (scriptSession != null) scriptSession.interfaceAttached();
	}

	void detach(long timeout) throws InterruptedException {
		checkAttached();
		attached = false;

		if (scriptSession != null) scriptSession.interfaceDetached();

		try {
			manager.halt(timeout);
		} finally {
			manager = null;

			if (directory != null) {
				directory.deregisterAll();
				directory = null;
			}

			if (deviceCaptured) {
				device.relinquish();
			}
		}
	}

	Screen screen() {
		checkAttached();
		return screen;
	}

	Directory directory() {
		checkAttached();
		return directory;
	}

	Qualifier qualifier() {
		checkAttached();
		return qualifier;
	}

	// private helper methods

	private void checkNotAttached() {
		if (attached) throw new IllegalStateException("already attached");
	}

	private void checkAttached() {
		if (!attached) throw new IllegalStateException("not attached");
	}

	private AppInstance instantiate(Bundle bundle) {
		//TODO should do some privilege checking here?
		Class<?> appClass = bundle.appClass();
		logger.debug().message("instantiating application class {} for instance {}").values(appClass, bundle.appData().appDetails()).log();
		if (appClass == null) {
			logger.error().message("application class unspecified for instance {}").values(bundle.instanceId).filePath(bundle.files().uri()).log();
			return instantiateBadApp(bundle.instanceId, "There was no application class specified.", logger);
		} else {
			for (AppHandler handler : runtime.appHandlers) {
				if (handler.handles(appClass)) {
					AppInstance instance = handler.instantiate(appClass, bundle, logger);
					return instance == null ? instantiateBadApp(bundle.instanceId, "Failed to to instantiate the application.", logger) : instance;
				}
			}
			return instantiateBadApp(bundle.instanceId, "Unsupported application class.", logger);
		}
	}

	private AppInstance instantiateBadApp(String instanceId, String reason, Logger logger) {
		Application application = new BadApplication(instanceId, reason);
		Bundle bundle = new BundleCollation(instanceId, appInstalls.badBundleFiles, logger, appInstalls.classLoader, true).bundle;
		return new AppInstance(instanceId, bundle, runtime.runtimeAppHandler, application);
	}

}
