package com.superdashi.gosper.micro;

@FunctionalInterface
public interface Activity {

	//TODO defining State here makes imports messy, move
	enum State {
		CONSTRUCTED, INITIALIZED, OPEN, ACTIVE;
	}

	//TODO binding device to context prevents device changes during activity lifetime
	default void init() {}

	//TODO should separate out opening from layout so that re-layout can be requested
	void open(DataInput savedState);

	default void activate() {}

	default void passivate() {}

	default void close(DataOutput state) {}

	default void destroy() {}

	// may return null to refuse relaunch, but should this be explicitly supported?
	default State relaunch(DataInput launchData) {
		return State.CONSTRUCTED;
	}

}
