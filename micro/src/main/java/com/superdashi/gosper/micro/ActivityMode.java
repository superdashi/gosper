package com.superdashi.gosper.micro;

public enum ActivityMode {

	SUCCEED_TOP, // this is the default
	REPLACE_TOP,
	SUCCEED_CURRENT,
	REPLACE_CURRENT,
	RELAUNCH_EXISTING_OR_SUCCEED_TOP,
	RELAUNCH_EXISTING_OR_REPLACE_TOP,
	RELAUNCH_EXISTING_OR_SUCCEED_CURRENT,
	RELAUNCH_EXISTING_OR_REPLACE_CURRENT,
	RESPOND_TO_TOP,
	RESPOND_TO_CURRENT,
	DETATCH;

	private static final ActivityMode[] values = values();

	static ActivityMode valueOf(int ordinal) {
		if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid ordinal");
		return values[ordinal];
	}

	// indicates that the activity should succeed an activity
	boolean succeed() {
		switch (this) {
		case SUCCEED_TOP                          :
		case SUCCEED_CURRENT                      :
		case RELAUNCH_EXISTING_OR_SUCCEED_TOP     :
		case RELAUNCH_EXISTING_OR_SUCCEED_CURRENT :
			return true;
			default: return false;
		}
	}

	// indicates that the activity should replace an activity
	boolean replace() {
		switch (this) {
		case REPLACE_TOP                          :
		case REPLACE_CURRENT                      :
		case RELAUNCH_EXISTING_OR_REPLACE_TOP     :
		case RELAUNCH_EXISTING_OR_REPLACE_CURRENT :
			return true;
			default: return false;
		}
	}

	// indicates that the activity should first attempt to relaunch an existing instance of the activity
	boolean relaunch() {
		switch(this) {
		case RELAUNCH_EXISTING_OR_SUCCEED_TOP     :
		case RELAUNCH_EXISTING_OR_REPLACE_TOP     :
		case RELAUNCH_EXISTING_OR_SUCCEED_CURRENT :
		case RELAUNCH_EXISTING_OR_REPLACE_CURRENT :
			return true;
			default: return false;
		}
	}

	// indicates that the activity is being requested to respond to another activity
	boolean respond() {
		switch (this) {
		case RESPOND_TO_TOP     :
		case RESPOND_TO_CURRENT :
			return true;
			default: return false;
		}
	}

	// indicates that the activity should be relative to the activity that was current when the launch was requested
	boolean current() {
		switch (this) {
			case SUCCEED_CURRENT                      :
			case REPLACE_CURRENT                      :
			case RELAUNCH_EXISTING_OR_SUCCEED_CURRENT :
			case RELAUNCH_EXISTING_OR_REPLACE_CURRENT :
			case RESPOND_TO_CURRENT                   :
				return true;
				default: return false;
		}
	}

	// indicates that the activity should be relative to the activity that is currently on top
	boolean top() {
		switch (this) {
		case SUCCEED_TOP                      :
		case REPLACE_TOP                      :
		case RELAUNCH_EXISTING_OR_SUCCEED_TOP :
		case RELAUNCH_EXISTING_OR_REPLACE_TOP :
		case RESPOND_TO_TOP                   :
			return true;
			default: return false;
		}
	}

}
