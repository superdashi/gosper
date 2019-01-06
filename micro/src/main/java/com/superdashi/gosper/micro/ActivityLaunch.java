package com.superdashi.gosper.micro;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;

import com.superdashi.gosper.framework.Identity;

// collects together the info needed to launch an activity
final class ActivityLaunch {

	final AppInstance appInstance;
	final ActivityInstance activityInstance;
	final boolean relaunch;
	final DataInput input; // caches deferred output converted to input
	final String respondToComponent;
	final String requestId;
	final ActivityMode mode;
	final int[] currentAncestorIds;

	ActivityLaunch(AppInstance appInstance, ActivityInstance activityInstance, boolean relaunch, DeferredActivity deferred) {
		this.appInstance = appInstance;
		this.activityInstance = activityInstance;
		this.input = deferred.launchData == null ? DataInput.empty : deferred.launchData.toInput();
		this.relaunch = relaunch;
		assert deferred != null;
		this.respondToComponent = deferred.respondToComponent;
		this.requestId = deferred.requestId;
		this.mode = deferred.mode;
		this.currentAncestorIds = deferred.currentAncestorIds;
	}

	// used to remove relaunch flag
	private ActivityLaunch(ActivityLaunch that, ActivityInstance activityInstance) {
		this.appInstance = that.appInstance;
		this.activityInstance = activityInstance;
		this.relaunch = false;
		this.input = that.input;
		this.respondToComponent = that.respondToComponent;
		this.requestId = that.requestId;
		this.mode = that.mode;
		this.currentAncestorIds = that.currentAncestorIds;
	}

	// returns null if activityCreator returns null
	Optional<ActivityLaunch> notRelaunch(BiFunction<Identity, String, Optional<ActivityInstance>> activityCreator) {
		Identity appIdentity = appInstance.details.identity();
		return activityCreator.apply(appIdentity, activityInstance.identity.name).map(a -> new ActivityLaunch(this, a));
	}

	@Override
	public String toString() {
		return String.format("activity: %s, relaunch: %b, component: %s, requestId: %s, mode: %s, launch ancestry: %s", activityInstance.identity, relaunch, respondToComponent, requestId, mode, Arrays.toString(currentAncestorIds));
	}
}