package com.superdashi.gosper.pimoroni;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.KeySet;
import com.superdashi.gosper.device.Keyboard;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.device.DeviceSpec.Builder;
import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.logging.Loggers;
import com.superdashi.gosper.pimoroni.CAP1166.TouchEvent;
import com.tomgibara.fundament.Producer;

public final class PimoroniGfxHat implements Device {

	private static final int BUT_UP     = 0;
	private static final int BUT_DOWN   = 1;
	private static final int BUT_BACK   = 2;
	private static final int BUT_LEFT   = 3;
	private static final int BUT_ACTION = 4;
	private static final int BUT_RIGHT  = 5;

	public enum Led {

		FAR_LEFT    (2),
		LEFT        (1),
		CENTER_LEFT (0),
		CENTER_RIGHT(5),
		RIGHT       (4),
		FAR_RIGHT   (3);

		final int index;

		private Led(int index) {
			this.index = index;
		}
	}

	// fields

	//TODO need a lifetime point to shutdown service
	private final ScheduledExecutorService busExecutor;
	private final Wifi wifi;
	private final int[] keys = new int[6];
	{
		keys[BUT_UP    ] = Event.KEY_UP     ;
		keys[BUT_DOWN  ] = Event.KEY_DOWN   ;
		keys[BUT_BACK  ] = Event.KEY_CANCEL ;
		keys[BUT_LEFT  ] = Event.KEY_LEFT   ;
		keys[BUT_ACTION] = Event.KEY_CONFIRM;
		keys[BUT_RIGHT ] = Event.KEY_RIGHT  ;
	}

	private final PimoroniGfxHatScreen screen;
	private final CAP1166 cap1166;

	private final DeviceSpec spec;
	private final Logger logger;

	private boolean captured = false;

	// constructors

	public PimoroniGfxHat(Loggers loggers, Wifi wifi) {
		logger = loggers.loggerFor("pimoroni_gfxhat");
		busExecutor = Executors.newSingleThreadScheduledExecutor();
		this.wifi = wifi;
		screen = new PimoroniGfxHatScreen(logger.child("screen"), new ST7567(), new SN3218());
		cap1166 = new CAP1166(busExecutor, 1, 0x2c);
		Builder builder = DeviceSpec.newBuilder().addButtons().setScreen(ScreenClass.MICRO, ScreenColor.MONO).addScreenAmbience().addScreenContrast().addScreenBrightness().addScreenInverted();
		if (wifi != null) builder.addWifi();
		//TODO this is a hack
		spec = builder.addKeyboard(Keyboard.newBuilder(KeySet.of(keys)).build()).build();
	}

	// device methods

	@Override
	public DeviceSpec getSpec() {
		return spec;
	}

	@Override
	public void capture() {
		if (captured) throw new IllegalStateException();
		debugLog("capturing");
		try {
			cap1166.start();
		} finally {
			captured = true;
		}
		debugLog("captured");
	}

	@Override
	public void relinquish() {
		if (!captured) throw new IllegalStateException();
		debugLog("relinquishing");
		try {
			cap1166.stop();
		} finally {
			captured = false;
		}
		debugLog("relinquished");
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
		return Optional.of(this::pollEvents);
	}

	@Override
	public void setEventConsumer(Consumer<Event> eventConsumer) {
		// noop
	}

	@Override
	public void setSpecConsumer(Consumer<DeviceSpec> specConsumer) {
		// noop
	}

	// private helper methods

	private List<Event> pollEvents() {
		debugLog("polling events");
		List<TouchEvent> list = cap1166.pollForEvents();
		switch (list.size()) {
		case 0: return Collections.emptyList();
		case 1: return Collections.singletonList(convert(list.get(0)));
		default: return list.stream().map(this::convert).collect(Collectors.toList());
		}
	}

	private Event convert(TouchEvent event) {
		return Event.newKeyEvent(keys[event.index()], event.pressed(), event.timestamp());
	}

	private void debugLog(String msg) {
		logger.debug().message(msg).log();
	}

}
