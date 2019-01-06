package com.superdashi.gosper.adafruit;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.InputEventListener;
import com.superdashi.gosper.device.Device;
import com.superdashi.gosper.device.DeviceSpec;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.device.network.Wifi;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.logging.Loggers;
import com.tomgibara.fundament.Producer;

public class Adafruit3531 implements Device {

	// mapping

	private static final int PIN_A =  5; // GPIO_21
	private static final int PIN_B =  6; // GPIO_22

	private static final int PIN_L = 27; // GPIO_02
	private static final int PIN_R = 23; // GPIO_04
	private static final int PIN_U = 17; // GPIO_00
	private static final int PIN_D = 22; // GPIO_03
	private static final int PIN_C =  4; // GPIO_07

	private static final int PIN_RST_SCR = 24; // GPIO_05;

	// statics

	private static final DeviceSpec baseSpec = DeviceSpec.newBuilder().addDPad().addButtons().setScreen(ScreenClass.MICRO, ScreenColor.MONO).addScreenContrast().addScreenInverted().build();

	// pins

	private final DigitalInputDevice aIn;
	private final DigitalInputDevice bIn;

	private final DigitalInputDevice lIn;
	private final DigitalInputDevice rIn;
	private final DigitalInputDevice uIn;
	private final DigitalInputDevice dIn;
	private final DigitalInputDevice cIn;

	private final DigitalOutputDevice rOut;

	private final Logger logger;
	private final Map<DigitalInputDevice, InputEventListener<DigitalInputEvent>> listeners = new IdentityHashMap<>();
	private final Adafruit3531Screen screen;
	private final Wifi wifi;
	private final DeviceSpec spec;

	private Consumer<Event> eventConsumer;

	public Adafruit3531(Loggers loggers, int i2cBus, Wifi wifi) {
		this.wifi = wifi;
		logger = loggers.loggerFor("adafruit3531");

		aIn = new DigitalInputDevice(PIN_A, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);
		bIn = new DigitalInputDevice(PIN_B, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);

		lIn = new DigitalInputDevice(PIN_L, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);
		rIn = new DigitalInputDevice(PIN_R, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);
		uIn = new DigitalInputDevice(PIN_U, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);
		dIn = new DigitalInputDevice(PIN_D, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);
		cIn = new DigitalInputDevice(PIN_C, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);

		rOut = new DigitalOutputDevice(PIN_RST_SCR);

		screen = new Adafruit3531Screen(logger.child("screen"), i2cBus, rOut);

		spec = wifi == null ? baseSpec : baseSpec.builder().addWifi().build();
	}

	// device methods

	@Override
	public void capture() {
		attachListener(aIn, Event.KEY_CONFIRM);
		attachListener(bIn, Event.KEY_CANCEL);

		attachListener(lIn, Event.KEY_LEFT);
		attachListener(rIn, Event.KEY_RIGHT);
		attachListener(uIn, Event.KEY_UP);
		attachListener(dIn, Event.KEY_DOWN);
		attachListener(cIn, Event.KEY_CENTER);
	}

	@Override
	public void relinquish() {
		listeners.forEach((p,l) -> p.removeListener(l));
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
		this.eventConsumer = eventConsumer;
	}

	@Override
	public DeviceSpec getSpec() {
		return spec;
	}

	@Override
	public void setSpecConsumer(Consumer<DeviceSpec> specConsumer) {
		// spec does not change at present
	}

	// private helper methods

	private void attachListener(DigitalInputDevice pin, int key) {
		InputEventListener<DigitalInputEvent> listener = newListener(key);
		pin.addListener(listener);
		listeners.put(pin, listener);
	}

	private InputEventListener<DigitalInputEvent> newListener(int key) {
		return e -> {
			if (eventConsumer != null) {
				eventConsumer.accept(Event.newKeyEvent(key, e.getValue()));
			}
		};
	}
}
