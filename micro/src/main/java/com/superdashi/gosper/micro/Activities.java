package com.superdashi.gosper.micro;

import com.superdashi.gosper.item.Item;

public final class Activities {

	static final String ACTIVITY_ID_DIALOG   = "gosper_runtime:dialog"  ;
	static final String ACTIVITY_ID_KEYBOARD = "gosper_runtime:keyboard";
	static final String ACTIVITY_ID_SELECT   = "gosper_runtime:select"  ;

	//TODO should return optional
	static ActivityInstance instantiatedStandardActivity(String activityId) {
		switch (activityId) {
		case ACTIVITY_ID_DIALOG  : return new ActivityInstance(new DialogActivity()  );
		case ACTIVITY_ID_KEYBOARD: return new ActivityInstance(new KeyboardActivity());
		case ACTIVITY_ID_SELECT  : return new ActivityInstance(new SelectActivity()  );
			default: return null;
		}
	}

	private final ActivityContext context;

	Activities(ActivityContext context) {
		this.context = context;
	}

	// public methods

	//TODO rename to just request?
	public ActivityRequest requestActivity(String activityId) {
		return new ActivityRequest(context, activityId);
	}

	public ActivityRequest requestKeyboard() {
		return new ActivityRequest(context, keyboard(), false);
	}

	public ActivityRequest requestKeyboard(Item info, String text, Regex regex) {
		if (info == null) throw new IllegalArgumentException("null info");
		if (text == null) throw new IllegalArgumentException("null text");
		return new ActivityRequest(context, keyboard(info, text, regex), false);
	}

	public ActivityRequest requestSelect() {
		return new ActivityRequest(context, select(), false);
	}

	public ActivityRequest requestSelect(Item info, int selected, Action... options) {
		if (info == null) throw new IllegalArgumentException("null info");
		if (options == null) throw new IllegalArgumentException("null options");
		return new ActivityRequest(context, select(info, selected, options), false);
	}

	public ActivityRequest requestDialog() {
		return new ActivityRequest(context, dialog(), false);
	}

	public ActivityRequest requestDialog(Item info, Action cancel, Action... options) {
		return new ActivityRequest(context, dialog(info, cancel, options), false);
	}

	// package scoped methods

	DeferredActivity keyboard() {
		return standard(ACTIVITY_ID_KEYBOARD, null);
	}

	DeferredActivity keyboard(Item info, String text, Regex regex) {
		return standard(ACTIVITY_ID_KEYBOARD, KeyboardActivity.dataFor(info, text, regex));
	}

	DeferredActivity select() {
		return standard(ACTIVITY_ID_SELECT, null);
	}

	DeferredActivity select(Item info, int selected, Action... options) {
		return standard(ACTIVITY_ID_SELECT, SelectActivity.dataFor(info, selected, options));
	}

	DeferredActivity dialog() {
		return standard(ACTIVITY_ID_DIALOG, null);
	}

	DeferredActivity dialog(Item info, Action cancel, Action... options) {
		return standard(ACTIVITY_ID_DIALOG, DialogActivity.dataFor(info, cancel, options));
	}

	// private helper methods

	private DeferredActivity standard(String activityId, DataOutput launchData) {
		DeferredActivity deferred = new DeferredActivity(activityId, context.appIdentity(), null, context.ancestorIds());
		// TODO is this the best default?
		deferred.mode = ActivityMode.RESPOND_TO_TOP;
		deferred.launchData = launchData;
		return deferred;
	}

}
