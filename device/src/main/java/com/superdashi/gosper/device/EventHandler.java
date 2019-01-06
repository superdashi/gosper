package com.superdashi.gosper.device;

@FunctionalInterface
public interface EventHandler {

	// returns true if the event was handled
	boolean handleEvent(Event event);
}
