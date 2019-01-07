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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.logging.Logger;

// stores application instances

class Directory implements Micro {

	private final Map<Application, Environment> envsByApp = new IdentityHashMap<>();
	private final Map<String, Environment> envsByInstance = new HashMap<>();
	private final Map<Identity, Environment> envsByIdentity = new HashMap<>();
	private final Interface face;
	private final Logger logger;

	Directory(Interface face) {
		this.face = face;
		logger = face.logger.child("directory");
	}

	// micro methods

	@Override
	public synchronized List<Details> allAppDetails() {
		//TODO could cache until change
		ArrayList<Details> details = new ArrayList<>(envsByInstance.size());
		for (Environment env : envsByApp.values()) {
			details.add(env.appInstance.details);
		}
		return Collections.unmodifiableList(details);
	}

	@Override
	public synchronized List<MicroAppLauncher> appLaunchers(Details appDetails, Qualifier qualifier) {
		if (qualifier == null) throw new IllegalArgumentException("null qualifier");
		if (!qualifier.isFullySpecified()) throw new IllegalArgumentException("qualifier not fully specified");
		if (appDetails == null) throw new IllegalArgumentException("null appDetails");
		Environment env = envsByIdentity.get(appDetails.identity());
		if (env == null) return Collections.emptyList();
		AppInstance appInstance = env.appInstance;
		if (!appInstance.details.equals(appDetails)) return Collections.emptyList();
		Bundle appData = appInstance.bundle;
		List<String> ids = appData.launchActivityIds();
		if (ids.isEmpty()) return Collections.emptyList();
		return Collections.unmodifiableList( ids.stream().map(id -> new MicroAppLauncher(qualifier, appData, id)).collect(Collectors.toList()) );
	}

	// lifetime methods

	synchronized boolean registerApp(AppInstance instance) {
		if (instance == null) throw new IllegalArgumentException("null install");
		Identity identity = instance.details.identity();
		// check for duplicates
		if (
				envsByApp.containsKey(instance.application) ||
				envsByInstance.containsKey(instance.instanceId) ||
				envsByIdentity.containsKey(identity)) {
			logger.warning().message("attempt to register existing application instance {} with identity {}").values(instance.instanceId, identity).log();
			return false;
		}
		Environment env = createEnvForApp(instance);
		envsByApp.put(instance.application, env);
		envsByInstance.put(instance.instanceId, env);
		envsByIdentity.put(identity, env);
		try {
			instance.application.init(env);
		} catch (RuntimeException e) {
			logger.error().message("error occurred initializing application instance {} with identity {}").values(instance.instanceId, identity).stacktrace(e).log();
		}
		return true;
	}

	//TODO should this take an env?
	synchronized boolean deregisterApp(Application app) {
		if (app == null) throw new IllegalArgumentException("null app");
		Environment env = envsByApp.get(app);
		if (env == null) return false;
		deregister(app, env);
		return true;
	}

	synchronized void deregisterAll() {
		new IdentityHashMap<>(envsByApp).forEach(this::deregister);
	}

	// accessors

	synchronized Optional<Environment> envForApp(Application app) {
		if (app == null) throw new IllegalArgumentException("null app");
		return Optional.ofNullable(envsByApp.get(app));
	}

	//TODO return optional
	synchronized Environment envForInstanceId(String instanceId) {
		if (instanceId == null) throw new IllegalArgumentException("null instanceId");
		Environment env = envsByInstance.get(instanceId);
		if (env == null) throw new IllegalArgumentException("instance not registered");
		return env;
	}

	synchronized Optional<Environment> envForIdentity(Identity identity) {
		if (identity == null) throw new IllegalArgumentException("null identity");
		return Optional.ofNullable( envsByIdentity.get(identity) );
	}

	// private helper methods

	private void deregister(Application app, Environment env) {
		AppInstance instance = env.appInstance;
		envsByApp.remove(instance.application);
		envsByInstance.remove(instance.instanceId);
		envsByIdentity.remove(instance.details.identity());
		try {
			app.destroy();
		} catch (RuntimeException e) {
			logger.error().message("error occurred destroying application instance {}").values(instance.instanceId).stacktrace(e).log();
		}
	}

	private Environment createEnvForApp(AppInstance instance) {
		return new Environment(face, instance);
	}

}
