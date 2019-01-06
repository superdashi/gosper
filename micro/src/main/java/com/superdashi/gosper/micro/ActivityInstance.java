package com.superdashi.gosper.micro;

import com.superdashi.gosper.framework.Identity;

// combines an activity with other supporting objects that may be created when the activity is instantiated
final class ActivityInstance {

	final Identity identity; // may be null for built-in activities
	final Activity activity;
	final ActionHandler defaultActionHandler;

	ActivityInstance(Activity activity) {
		this.identity = null;
		this.activity = activity;
		this.defaultActionHandler = null;
	}

	ActivityInstance(Identity identity, Activity activity, ActionHandler defaultActionHandler) {
		this.identity = identity;
		this.activity = activity;
		this.defaultActionHandler = defaultActionHandler;
	}

}
