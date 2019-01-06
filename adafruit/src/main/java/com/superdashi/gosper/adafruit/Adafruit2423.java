package com.superdashi.gosper.adafruit;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.diozero.api.PwmOutputDevice;
import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.DeviceException;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.linux.LinuxEvents;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.logging.Loggers;
import com.tomgibara.fundament.Producer;
import com.tomgibara.intgeom.IntCoords;

public class Adafruit2423 implements Device {

	// mapping

	private static final int PIN_BRI = 18; //GPIO 01

	// statics

	private static final long EVENT_TIMEOUT = 1000L;
	private static final DeviceSpec baseSpec = DeviceSpec.newBuilder().addTouch().setScreen(ScreenClass.MINI, ScreenColor.COLOR).addScreenBrightness().build();

	// fields

	private final Logger logger;
	private final Adafruit2423Screen screen;
	private final Wifi wifi;
	private final LinuxEvents events;
	private final DeviceSpec spec;

	public Adafruit2423(String screenDevice, Wifi wifi, Loggers loggers) {
		this.wifi = wifi;
		logger = loggers.loggerFor("adafruit2423");
		PwmOutputDevice briPin = new PwmOutputDevice(PIN_BRI);
		screen = new Adafruit2423Screen(screenDevice, briPin, logger.child("screen"));
		//TODO make these configurable
		events = new LinuxEvents("/dev/input/touchscreen", 5f, c -> IntCoords.at(320 - c.y, c.x));
		spec = wifi == null ? baseSpec : baseSpec.builder().addWifi().build();
	}

	@Override
	public DeviceSpec getSpec() {
		return spec;
	}

	@Override
	public void capture() {
		//TODO is this really a noop?
	}

	@Override
	public void relinquish() {
		//TODO is this really a noop?
	}

	@Override
	public Optional<Screen> getScreen() {
		return Optional.of(screen);
	}

	@Override
	public Optional<Wifi> getWifi() {
		return Optional.ofNullable(wifi);
	}

	@Override
	public Optional<Producer<List<Event>>> events() {
		return Optional.empty();
	}

	@Override
	public void setEventConsumer(Consumer<Event> eventConsumer) {
		try {
			if (events.started()) {
				events.stop(EVENT_TIMEOUT);
			}
			if (eventConsumer != null) {
				events.start(eventConsumer);
			}
		} catch (DeviceException e) {
			logger.error().message(eventConsumer == null ? "failed to clear event consumer" : "failed to set event consumer").stacktrace(e).log();
		}
	}

	@Override
	public void setSpecConsumer(Consumer<DeviceSpec> specConsumer) {
		// TODO Auto-generated method stub

	}

}
