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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;
import com.superdashi.gosper.data.DataContext;
import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.studio.Studio;
import com.superdashi.gosper.studio.StudioPlan;

public final class Interfaces {

	private static final long DETACH_TIMEOUT = 1000L; // in ms

	private static final Maps<String, Interface> interfaceMaps = Collect.setsOf(String.class).mappedTo(Interface.class);

	private final Runtime runtime;
	private final Logger logger;
	private final Object lock; // taken from appInstalls
	private final Map<String, Interface> interfaces = interfaceMaps.newMap();

	Interfaces(Runtime runtime) {
		this.runtime = runtime;
		logger = runtime.logger.child("interfaces");
		lock = runtime.appInstalls.lifetime;
	}

	// must be called with lock
	void start() {
		if (interfaces.isEmpty()) return; // nothing to do
		for (Interface face : interfaces.values()) {
			try {
				face.attach();
			} catch (InterruptedException e) {
				reportInterruption("interrupted starting up interface {}", face, e);
			}
		}
	}

	// must be called with lock
	void stop() {
		if (interfaces.isEmpty()) return; // nothing to do
		for (Interface face : interfaces.values()) {
			try {
				face.detach(DETACH_TIMEOUT);
			} catch (InterruptedException e) {
				reportInterruption("interrupted shutting down interface {}", face, e);
			}
		}
	}

	// dataContexts is optional
	public Interface addInterface(String identifier, Device device, Function<Identity, DataContext> dbConnector) {
		if (identifier == null) throw new IllegalArgumentException("null identifier");
		if (device == null) throw new IllegalArgumentException("null device");
		synchronized (lock) {
			if (interfaces.containsKey(identifier)) throw new IllegalArgumentException("interface already added with identifier " + identifier);
			//TODO need to build appropriate studio for device?
			Studio studio = new StudioPlan().createLocalStudio();
			Interface face = new Interface(runtime, identifier, device, dbConnector, studio);
			interfaces.put(identifier, face);
			if (runtime.started()) {
				try {
					face.attach();
				} catch (InterruptedException e) {
					reportInterruption("interrupted adding interface {}", face, e);
				}
			}
			return face;
		}
	}

	public Collection<Interface> toCollection() {
		synchronized (lock) {
			return Collections.unmodifiableList(new ArrayList<>(interfaces.values()));
		}
	}

	public void removeInterface(String identifier) {
		if (identifier == null) throw new IllegalArgumentException("null identifier");
		synchronized (lock) {
			Interface face = interfaces.remove(identifier);
			if (face == null) throw new IllegalArgumentException("no interface with identifier: " + identifier);
			if (runtime.started()) {
				try {
					face.detach(DETACH_TIMEOUT);
				} catch (InterruptedException e) {
					reportInterruption("interrupted removing interface {}", face, e);
				}
			}
			face.studio.close();
		}
	}

	private void reportInterruption(String message, Interface face, InterruptedException e) {
		logger.warning().message(message).values(face.identifier).stacktrace(e).log();
	}

}
