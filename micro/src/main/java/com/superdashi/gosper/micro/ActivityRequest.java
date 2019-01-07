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

import java.io.Serializable;

import com.superdashi.gosper.framework.Identity;
import com.tomgibara.streams.StreamSerializer;

public final class ActivityRequest {

	//TODO need a better place to record this
	//TODO need to prevent applications from using this prefix
	final static String RUNTIME_PREFIX = "gosper_runtime";

	private final ActivityContext context;
	private DeferredActivity deferred;
	private boolean exposed;

	ActivityRequest(ActivityContext context, String activityId) {
		this.context = context;

		Identity appIdentity;
		Identity activityIdentity;
		if (activityId == null) {
			appIdentity = null;
			activityIdentity = null;
		} else {
			int i = activityId.lastIndexOf(':');
			Environment env = context.environment();
			if (i == -1) { // local to application
				appIdentity = env.appData().appDetails().identity();
				activityIdentity = env.parseIdentity(activityId);
			} else if (activityId.substring(0, i).equals(RUNTIME_PREFIX)) { // possibly a valid runtime defined activity
				appIdentity = env.appData().appDetails().identity(); // define as local
				activityIdentity = null;
			} else { // possibly in another application
				appIdentity = env.parseIdentity(activityId.substring(0, i));
				activityIdentity = new Identity(appIdentity.ns, activityId.substring(i + 1));
			}
		}
		deferred = new DeferredActivity(activityId, appIdentity, activityIdentity, context.ancestorIds());
		exposed = false;
	}

	ActivityRequest(ActivityContext context, DeferredActivity deferred, boolean exposed) {
		this.context = context;
		this.deferred = deferred;
		this.exposed = exposed;
	}

	// public acccessors

	public String activityId() {
		return deferred.activityId;
	}

	public String requestId() {
		return deferred.requestId;
	}

	public ActivityRequest requestId(String requestId) {
		if (requestId != null && requestId.isEmpty()) requestId = null;
		cow();
		deferred.requestId = requestId;
		return this;
	}

	public DataOutput launchData() {
		return deferred.launchData;
	}

	public ActivityRequest launchData(DataOutput parameters) {
		cow();
		deferred.launchData = parameters;
		return this;
	}

	public ActivityMode mode() {
		return deferred.mode;
	}

	public ActivityRequest mode(ActivityMode mode) {
		if (mode == null) throw new IllegalArgumentException("null mode");
		cow();
		deferred.mode = mode;
		return this;
	}

	// public methods

	//TODO return some form of identifier maybe?
	public void launch() {
		exposed = true;
		context.launchActivity(deferred);
	}

	public DeferredActivity defer() {
		exposed = true;
		return deferred;
	}

	// convenience methods

	public ActivityRequest put(String key, Serializable value) {
		cow();
		deferred.certainLaunchData().put(key, value);
		return this;
	}

	public <V> ActivityRequest put(String key, V value, StreamSerializer<V> serializer) {
		cow();
		deferred.certainLaunchData().put(key, value, serializer);
		return this;
	}
	public ActivityRequest remove(String key) {
		cow();
		deferred.certainLaunchData().remove(key);
		return this;
	}

	// private helper methods

	private void cow() {
		if (exposed) {
			deferred = deferred.copy();
			exposed = false;
		}
	}

}
