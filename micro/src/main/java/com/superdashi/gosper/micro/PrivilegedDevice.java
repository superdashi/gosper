package com.superdashi.gosper.micro;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.superdashi.gosper.bundle.Privileges;
import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.device.network.Wifi;
import com.tomgibara.fundament.Producer;

final class PrivilegedDevice implements Device {

	private final Device device;
	private final Privileges privileges;

	PrivilegedDevice(Device device, Privileges privileges) {
		this.device = device;
		this.privileges = privileges;
	}

	@Override
	public DeviceSpec getSpec() {
		return device.getSpec();
	}

	@Override
	public void capture() {
		privileges.check(Privileges.SYSTEM);
		device.capture();
	}

	@Override
	public void relinquish() {
		privileges.check(Privileges.SYSTEM);
		device.relinquish();
	}

	@Override
	public Optional<Screen> getScreen() {
		return device.getScreen().map(s -> new PrivilegedScreen(s, privileges));
	}

	@Override
	public Optional<Wifi> getWifi() {
		return device.getWifi().map(w -> new PrivilegedWifi(w, privileges));
	}

	@Override
	public Optional<Producer<List<Event>>> events() {
		privileges.check(Privileges.UNAVAILABLE);
		return device.events();
	}

	@Override
	public void setEventConsumer(Consumer<Event> eventConsumer) {
		privileges.check(Privileges.UNAVAILABLE);
		device.setEventConsumer(eventConsumer);
	}

	@Override
	public void setSpecConsumer(Consumer<DeviceSpec> specConsumer) {
		privileges.check(Privileges.UNAVAILABLE);
		device.setSpecConsumer(specConsumer);
	}

}