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

import com.superdashi.gosper.bundle.Bundle;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Qualifier;

public final class MicroAppLauncher {

	private final Identity appIdentity;
	private final Identity activityIdentity;
	private final Item activityMeta;

	MicroAppLauncher(Qualifier qualifier, Bundle bundle, String activityId) {
		appIdentity = bundle.appData().appDetails().identity();
		activityIdentity = bundle.activityDetails(activityId).details.identity();
		activityMeta = bundle.activityMetaItem(qualifier, activityId);
	}

	public Identity appIdentity() {
		return appIdentity;
	}

	public Identity activityIdentity() {
		return activityIdentity;
	}

	public Item activityMeta() {
		return activityMeta;
	}

	public DeferredActivity defer(ActivityMode mode) {
		if (mode == null) throw new IllegalArgumentException("null mode");
		DeferredActivity deferred = new DeferredActivity(activityId(), appIdentity, activityIdentity, DeferredActivity.NO_ANCESTOR_IDS);
		deferred.mode = mode;
		return deferred;
	}

	public Action action() {
		return Action.create(activityId(), activityMeta, defer(ActivityMode.DETATCH));
	}

	public Action action(ActivityMode mode) {
		return Action.create(activityId(), activityMeta, defer(mode));
	}

	public Action action(String id, ActivityMode mode) {
		return Action.create(id, activityMeta, defer(mode));
	}

	private String activityId() {
		return activityIdentity.name;
	}
}
