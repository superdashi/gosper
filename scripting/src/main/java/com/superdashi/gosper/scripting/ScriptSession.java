package com.superdashi.gosper.scripting;

import java.util.function.Consumer;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.item.Item;

public interface ScriptSession {

	void interfaceAttached();
	void interfaceDetached();

	void eventConsumer(Consumer<Event> eventConsumer);

	void activityActivated(int activityId, Details details);
	void actionAbsent();
	void actionSelected(String id, Item item);
}
