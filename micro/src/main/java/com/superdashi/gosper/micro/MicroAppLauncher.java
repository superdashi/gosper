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
