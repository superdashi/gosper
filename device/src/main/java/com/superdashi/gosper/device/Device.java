package com.superdashi.gosper.device;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.superdashi.gosper.device.network.Wifi;
import com.tomgibara.fundament.Producer;

public interface Device {

	// may be called before capture
	DeviceSpec getSpec();

	// lifecycle
	void capture();
	void relinquish();

	Optional<Screen> getScreen();
	Optional<Wifi> getWifi();

	// events, only delivered after capture
	void setEventConsumer(Consumer<Event> eventConsumer);
	void setSpecConsumer(Consumer<DeviceSpec> specConsumer);

	// if non empty, the object from which events must be polled.
	Optional<Producer<List<Event>>> events();
}
