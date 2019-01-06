package com.superdashi.gosper.pimoroni;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.diozero.api.I2CDevice;

/*
 * Derived from: https://github.com/pimoroni/cap1xxx/blob/master/library/cap1xxx.py
 * Original copyright: 2017 Pimoroni Ltd.
 */

public class CAP1166 {

	// DEVICE MAP
	private static final int DEFAULT_ADDR = 0x28;

	// Supported devices
	private static final int PID_CAP1208 = 0b01101011;
	private static final int PID_CAP1188 = 0b01010000;
	private static final int PID_CAP1166 = 0b01010001;

	// REGISTER MAP

	private static final int R_MAIN_CONTROL      = 0x00;
	private static final int R_GENERAL_STATUS    = 0x02;
	private static final int R_INPUT_STATUS      = 0x03;
	private static final int R_LED_STATUS        = 0x04;
	private static final int R_NOISE_FLAG_STATUS = 0x0A;

	// Read-only delta counts for all inputs
	private static final int R_INPUT_1_DELTA   = 0x10;
	private static final int R_INPUT_2_DELTA   = 0x11;
	private static final int R_INPUT_3_DELTA   = 0x12;
	private static final int R_INPUT_4_DELTA   = 0x13;
	private static final int R_INPUT_5_DELTA   = 0x14;
	private static final int R_INPUT_6_DELTA   = 0x15;
	private static final int R_INPUT_7_DELTA   = 0x16;
	private static final int R_INPUT_8_DELTA   = 0x17;

	private static final int R_SENSITIVITY     = 0x1F;
	// B7     = N/A
	// B6..B4 = Sensitivity
	// B3..B0 = Base Shift
	//private static final int[] SENSITIVITY = {128: 0b000, 64:0b001, 32:0b010, 16:0b011, 8:0b100, 4:0b100, 2:0b110, 1:0b111}

	private static final int R_GENERAL_CONFIG  = 0x20;
	// B7 = Timeout
	// B6 = Wake Config ( 1 = Wake pin asserted )
	// B5 = Disable Digital Noise ( 1 = Noise threshold disabled )
	// B4 = Disable Analog Noise ( 1 = Low frequency analog noise blocking disabled )
	// B3 = Max Duration Recalibration ( 1 =  Enable recalibration if touch is held longer than max duration )
	// B2..B0 = N/A

	private static final int R_INPUT_ENABLE    = 0x21;


	private static final int R_INPUT_CONFIG    = 0x22;

	private static final int R_INPUT_CONFIG2   = 0x23; // Default 0x00000111

	// Values for bits 3 to 0 of R_INPUT_CONFIG2
	// Determines minimum amount of time before
	// a "press and hold" event is detected.

	// Also - Values for bits 3 to 0 of R_INPUT_CONFIG
	// Determines rate at which interrupt will repeat
	//
	// Resolution of 35ms, max = 35 + (35 * 0b1111) = 560ms

	private static final int R_SAMPLING_CONFIG = 0x24; // Default 0x00111001
	private static final int R_CALIBRATION     = 0x26; // Default 0b00000000
	private static final int R_INTERRUPT_EN    = 0x27; // Default 0b11111111
	private static final int R_REPEAT_EN       = 0x28; // Default 0b11111111
	private static final int R_MTOUCH_CONFIG   = 0x2A; // Default 0b11111111
	private static final int R_MTOUCH_PAT_CONF = 0x2B;
	private static final int R_MTOUCH_PATTERN  = 0x2D;
	private static final int R_COUNT_O_LIMIT   = 0x2E;
	private static final int R_RECALIBRATION   = 0x2F;

	// R/W Touch detection thresholds for inputs
	private static final int R_INPUT_1_THRESH  = 0x30;
	private static final int R_INPUT_2_THRESH  = 0x31;
	private static final int R_INPUT_3_THRESH  = 0x32;
	private static final int R_INPUT_4_THRESH  = 0x33;
	private static final int R_INPUT_5_THRESH  = 0x34;
	private static final int R_INPUT_6_THRESH  = 0x35;
	private static final int R_INPUT_7_THRESH  = 0x36;
	private static final int R_INPUT_8_THRESH  = 0x37;

	// R/W Noise threshold for all inputs
	private static final int R_NOISE_THRESH    = 0x38;

	// R/W Standby and Config Registers
	private static final int R_STANDBY_CHANNEL = 0x40;
	private static final int R_STANDBY_CONFIG  = 0x41;
	private static final int R_STANDBY_SENS    = 0x42;
	private static final int R_STANDBY_THRESH  = 0x43;

	private static final int R_CONFIGURATION2  = 0x44;
	// B7 = Linked LED Transition Controls ( 1 = LED trigger is !touch )
	// B6 = Alert Polarity ( 1 = Active Low Open Drain, 0 = Active High Push Pull )
	// B5 = Reduce Power ( 1 = Do not power down between poll )
	// B4 = Link Polarity/Mirror bits ( 0 = Linked, 1 = Unlinked )
	// B3 = Show RF Noise ( 1 = Noise status registers only show RF, 0 = Both RF and EMI shown )
	// B2 = Disable RF Noise ( 1 = Disable RF noise filter )
	// B1..B0 = N/A

	// Read-only reference counts for sensor inputs
	private static final int R_INPUT_1_BCOUNT  = 0x50;
	private static final int R_INPUT_2_BCOUNT  = 0x51;
	private static final int R_INPUT_3_BCOUNT  = 0x52;
	private static final int R_INPUT_4_BCOUNT  = 0x53;
	private static final int R_INPUT_5_BCOUNT  = 0x54;
	private static final int R_INPUT_6_BCOUNT  = 0x55;
	private static final int R_INPUT_7_BCOUNT  = 0x56;
	private static final int R_INPUT_8_BCOUNT  = 0x57;

	// LED Controls - For CAP1188 and similar
	private static final int R_LED_OUTPUT_TYPE = 0x71;
	private static final int R_LED_LINKING     = 0x72;
	private static final int R_LED_POLARITY    = 0x73;
	private static final int R_LED_OUTPUT_CON  = 0x74;
	private static final int R_LED_LTRANS_CON  = 0x77;
	private static final int R_LED_MIRROR_CON  = 0x79;

	// LED Behaviour
	private static final int R_LED_BEHAVIOUR_1 = 0x81; // For LEDs 1-4
	private static final int R_LED_BEHAVIOUR_2 = 0x82; // For LEDs 5-8
	private static final int R_LED_PULSE_1_PER = 0x84;
	private static final int R_LED_PULSE_2_PER = 0x85;
	private static final int R_LED_BREATHE_PER = 0x86;
	private static final int R_LED_CONFIG      = 0x88;
	private static final int R_LED_PULSE_1_DUT = 0x90;
	private static final int R_LED_PULSE_2_DUT = 0x91;
	private static final int R_LED_BREATHE_DUT = 0x92;
	private static final int R_LED_DIRECT_DUT  = 0x93;
	private static final int R_LED_DIRECT_RAMP = 0x94;
	private static final int R_LED_OFF_DELAY   = 0x95;

	// R/W Power buttonc ontrol
	private static final int R_POWER_BUTTON    = 0x60;
	private static final int R_POW_BUTTON_CONF = 0x61;

	// Read-only upper 8-bit calibration values for sensors
	private static final int R_INPUT_1_CALIB   = 0xB1;
	private static final int R_INPUT_2_CALIB   = 0xB2;
	private static final int R_INPUT_3_CALIB   = 0xB3;
	private static final int R_INPUT_4_CALIB   = 0xB4;
	private static final int R_INPUT_5_CALIB   = 0xB5;
	private static final int R_INPUT_6_CALIB   = 0xB6;
	private static final int R_INPUT_7_CALIB   = 0xB7;
	private static final int R_INPUT_8_CALIB   = 0xB8;

	// Read-only 2 LSBs for each sensor input
	private static final int R_INPUT_CAL_LSB1  = 0xB9;
	private static final int R_INPUT_CAL_LSB2  = 0xBA;

	// Product ID Registers
	private static final int R_PRODUCT_ID      = 0xFD;
	private static final int R_MANUFACTURER_ID = 0xFE;
	private static final int R_REVISION        = 0xFF;

	// LED Behaviour settings
	private static final int LED_BEHAVIOUR_DIRECT  = 0b00;
	private static final int LED_BEHAVIOUR_PULSE1  = 0b01;
	private static final int LED_BEHAVIOUR_PULSE2  = 0b10;
	private static final int LED_BEHAVIOUR_BREATHE = 0b11;

	private static final int LED_OPEN_DRAIN = 0; // Default, LED is open-drain output with ext pullup
	private static final int LED_PUSH_PULL  = 1; // LED is driven HIGH/LOW with logic 1/0

	private static final int LED_RAMP_RATE_2000MS = 7;
	private static final int LED_RAMP_RATE_1500MS = 6;
	private static final int LED_RAMP_RATE_1250MS = 5;
	private static final int LED_RAMP_RATE_1000MS = 4;
	private static final int LED_RAMP_RATE_750MS  = 3;
	private static final int LED_RAMP_RATE_500MS  = 2;
	private static final int LED_RAMP_RATE_250MS  = 1;
	private static final int LED_RAMP_RATE_0MS    = 0;

	private static int scaleMillis(int ms) {
		int v = Math.round(ms / 35f) - 1;
		return Math.min(Math.max(0, v), 15);
	}

	private static final int EVENT_QUEUE_INIT_SIZE = 16;
	private static final int EVENT_QUEUE_MAX_SIZE = 1024;
	private static final long INTERRUPT_POLL_PERIOD = 50L;

	private final ScheduledExecutorService busExector;

	private final int numberOfInputs;
	private final int numberOfLeds;
	private final int i2cAdr;
	private final int i2cBus;

	private I2CDevice i2c = null;
	private ScheduledFuture<?> polling;

	private final Object eventLock = new Object();
	// events are packed as:
	//   60 bits time
	//    3 bits id
	//    1 bit pressed

	private long[] events = new long[EVENT_QUEUE_INIT_SIZE];
	private int firstEvent = 0;
	private int finalEvent = 0;

	private int previousState = 0;

	private CAP1166(int numberOfInputs, int numberOfLeds, ScheduledExecutorService busExecutor, int i2cBus, int i2cAdr) {
		this.numberOfInputs = numberOfInputs;
		this.numberOfLeds = numberOfLeds;
		this.busExector = busExecutor;
		this.i2cAdr = i2cAdr;
		this.i2cBus = i2cBus;
	}

	public CAP1166(ScheduledExecutorService service, int i2cBus, int i2cAdr) {
		this(6, 6, service, i2cBus, i2cAdr);
	}

	public void start() {
		if (i2c != null) throw new IllegalStateException();

		// configure buttons
		executeOnBus(() -> {
			i2c = new I2CDevice(i2cBus, i2cAdr);

			i2c.writeByte(R_INPUT_ENABLE, -1); // input active on all buttons
			i2c.writeByte(R_INTERRUPT_EN, -1); // support interrupts on all buttons
			//TODO for now, we won't support repeats, but we will
			i2c.writeByte(R_REPEAT_EN, 0);
			byte mt = i2c.readByte(R_MTOUCH_CONFIG); // retrieve config flags
			i2c.writeByte(R_MTOUCH_CONFIG, mt | 0x80); // and enable multitouch

			setHoldDelay(210);
			setRepeatPeriod(210);

			// Tested sane defaults for various configurations
			i2c.writeByte(R_SAMPLING_CONFIG, 0b00001000); // 1sample per measure, 1.28ms time, 35ms cycle
			i2c.writeByte(R_SENSITIVITY,     0b01100000); // 2x sensitivity
			i2c.writeByte(R_GENERAL_CONFIG,  0b00111000);
			i2c.writeByte(R_CONFIGURATION2,  0b01100000);
		});

		// start polling
		polling = busExector.scheduleWithFixedDelay(this::pollForChanges, 0L, INTERRUPT_POLL_PERIOD, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (i2c == null) throw new IllegalStateException();
		polling.cancel(false);
		polling = null;
		i2c.close();
		i2c = null;
	}

	public Optional<TouchEvent> pollForEvent() {
		synchronized (eventLock) {
			if (firstEvent == finalEvent) return Optional.empty();
			long value = events[firstEvent];
			firstEvent = advanced(firstEvent);
			return Optional.of(new TouchEvent(value));
		}
	}

	public List<TouchEvent> pollForEvents() {
		synchronized (eventLock) {
			if (firstEvent == finalEvent) return Collections.emptyList();
			if (firstEvent == finalEvent - 1) return Collections.singletonList( new TouchEvent(events[firstEvent++]) );
			List<TouchEvent> list;
			if (firstEvent < finalEvent) {
				list = new ArrayList<>(finalEvent - firstEvent);
				for (int i = firstEvent; i < finalEvent; i++) {
					list.add(new TouchEvent(events[i]));
				}
			} else {
				list = new ArrayList<>(events.length + finalEvent - firstEvent);
				for (int i = firstEvent; i < events.length; i++) {
					list.add(new TouchEvent(events[i]));
				}
				for (int i = 0; i < finalEvent; i++) {
					list.add(new TouchEvent(events[i]));
				}
			}
			firstEvent = 0;
			finalEvent = 0;
			return list;
		}
	}

	// private helper methods

	private void executeOnBus(Runnable r) {
		try {
			busExector.submit(r).get();
		} catch (ExecutionException e) {
			throw (RuntimeException) e.getCause();
		} catch (InterruptedException e) {
			throw new RuntimeException("interrupted during bus execution");
		}
	}

	private void pollForChanges() {
		// check for an interrupt
		if (!pollInterrupt()) return;
		// clear interrupt state
		clearInterrupt();
		// get the current state
		// NOTE: this has to be done *before* reading the state, otherwise the state will record released keys as down
		int state = i2c.readByte(R_INPUT_STATUS) & 0xff;
		// diff against previous state to populate event queue
		long timestamp = System.currentTimeMillis();
		for (int i = 0; i < numberOfInputs; i++) {
			int mask = 1 << i;
			boolean pressedNow = (state & mask) != 0;
			boolean pressedThen = (previousState & mask) != 0;
			if (pressedNow == pressedThen) continue;
			recordEvent(i, pressedNow, timestamp);
		}
		// record state as previous state
		previousState = state;
	}

	private void setHoldDelay(int ms) {
		int value = i2c.readByte(R_INPUT_CONFIG2);
		value = (value & 0xf0) | scaleMillis(ms);
		i2c.writeByte(R_INPUT_CONFIG2, value);
	}

	private void setRepeatPeriod(int ms) {
		int value = i2c.readByte(R_INPUT_CONFIG);
		value = (value & 0xf0) | scaleMillis(ms);
		i2c.writeByte(R_INPUT_CONFIG, value);
	}

	private boolean pollInterrupt() {
		return i2c.readBit(R_MAIN_CONTROL, 0);
	}

	private void clearInterrupt() {
		i2c.writeBit(R_MAIN_CONTROL, 0, false);
	}

	private void recordEvent(int index, boolean pressed, long timestamp) {
		synchronized (eventLock) {
			long value = (timestamp << 3 | index) << 1 | (pressed ? 1 : 0);
			int nextEvent = advanced(finalEvent);
			int length = events.length;
			if (nextEvent == firstEvent) { // would overflow
				if (events.length >= EVENT_QUEUE_MAX_SIZE) {
					// we have to drop oldest event
					firstEvent = advanced(firstEvent);
					//TODO should log warning
				} else {
					// we can grow the buffer...
					if (firstEvent == 0) {
						// ... by range
						events = Arrays.copyOfRange(events, 0, length * 2);
					} else {
						// by copying
						long[] newEvents = new long[events.length * 2];
						System.arraycopy(events, firstEvent, newEvents, 0, length - firstEvent);
						System.arraycopy(events, 0, newEvents, length - firstEvent, firstEvent - 1);
						events = newEvents;
					}
					nextEvent = length;
					// finalEvent must already be length - 1
				}
			}
			events[finalEvent] = value;
			finalEvent = nextEvent;
		}
	}

	// must be called with eventLock
	private int advanced(int position) {
		position++;
		return position < events.length ? position : 0;
	}

	static public final class TouchEvent {

		private final long value;

		TouchEvent(long value) {
			this.value = value;
		}

		public boolean pressed() {
			return (value & 1) != 0;
		}

		public int index() {
			return (int) value >> 1 & 7;
		}

		public long timestamp() {
			return value >> 4;
		}
	}
}
