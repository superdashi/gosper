package com.superdashi.gosper.device;

import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;

// TODO should have screen spec?

public class DeviceSpec {

	public static class Builder {

		private int flags;
		private Keyboard keyboard;
		private ScreenClass screenClass;
		private ScreenColor screenColor;

		public Builder() {
			flags = 0;
			keyboard = Keyboard.withNoKeys();
			screenClass = ScreenClass.NONE;
			screenColor = ScreenColor.OTHER;
		}

		private Builder(DeviceSpec device) {
			flags = device.flags;
			keyboard = device.keyboard;
			screenClass = device.screenClass;
			screenColor = device.screenColor;
		}

		// input

		public Builder addDPad() {
			flags |= FLAGS_DPAD;
			return this;
		}

		public Builder addButtons() {
			flags |= FLAGS_BUTTONS;
			return this;
		}

		public Builder addKeyboard(Keyboard keyboard) {
			if (keyboard == null) throw new IllegalArgumentException("null keyboard");
			this.keyboard = keyboard;
			flags |= FLAGS_KEYBOARD;
			return this;
		}

		public Builder addMouse() {
			flags |= FLAGS_MOUSE;
			return this;
		}

		public Builder addTouch() {
			flags |= FLAGS_TOUCH;
			return this;
		}

		public Builder addScreenInverted() {
			flags |= FLAGS_SCR_INV;
			return this;
		}

		public Builder addScreenContrast() {
			flags |= FLAGS_SCR_CON;
			return this;
		}

		public Builder addScreenBrightness() {
			flags |= FLAGS_SCR_BRI;
			return this;
		}

		public Builder addScreenAmbience() {
			flags |= FLAGS_SCR_AMB;
			return this;
		}

		// output

		public Builder setScreen(ScreenClass screenClass, ScreenColor screenColor) {
			if (screenClass == ScreenClass.NONE && screenColor != ScreenColor.OTHER) {
				throw new IllegalArgumentException("screenColor must be OTHER for screenClass NONE");
			}
			this.screenClass = screenClass;
			this.screenColor = screenColor;
			return this;
		}

		public Builder addHdmi() {
			flags |= FLAGS_HDMI;
			return this;
		}

		// network

		public Builder addWifi() {
			flags |= FLAGS_WIFI;
			return this;
		}

		// methods

		public DeviceSpec build() {
			if (screenClass == ScreenClass.NONE) {
				// cannot have any screen flags without screen
				flags &= ~(FLAGS_SCREEN | FLAGS_SCR_ALL);
			} else {
				// must have screen flag set with screen
				flags |= FLAGS_SCREEN;
			}
			return new DeviceSpec(this);
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	// input

	private static final int FLAGS_DPAD     = 0b000000000001;
	private static final int FLAGS_BUTTONS  = 0b000000000010;
	private static final int FLAGS_KEYBOARD = 0b000000000100;
	private static final int FLAGS_MOUSE    = 0b000000001000;
	private static final int FLAGS_TOUCH    = 0b000000010000;

	// output

	private static final int FLAGS_SCREEN   = 0b000000100000;
	private static final int FLAGS_HDMI     = 0b000001000000;

	// network

	private static final int FLAGS_WIFI     = 0b000010000000;

	// screen

	private static final int FLAGS_SCR_INV  = 0b000100000000;
	private static final int FLAGS_SCR_CON  = 0b001000000000;
	private static final int FLAGS_SCR_BRI  = 0b010000000000;
	private static final int FLAGS_SCR_AMB  = 0b100000000000;
	private static final int FLAGS_SCR_ALL  = 0b111100000000;

	private final int flags;
	public final Keyboard keyboard;
	public final ScreenClass screenClass;
	public final ScreenColor screenColor;

	private DeviceSpec(Builder builder) {
		flags = builder.flags;
		keyboard = builder.keyboard;
		screenClass = builder.screenClass;
		screenColor = builder.screenColor;
	}

	// input

	public boolean hasDPad() {
		return isSet(FLAGS_DPAD);
	}

	public boolean hasButtons() {
		return isSet(FLAGS_BUTTONS);
	}

	public boolean hasKeyboard() {
		return isSet(FLAGS_KEYBOARD);
	}

	public boolean hasMouse() {
		return isSet(FLAGS_MOUSE);
	}

	public boolean hasTouch() {
		return isSet(FLAGS_TOUCH);
	}

	// output

	public boolean hasHdmi() {
		return isSet(FLAGS_HDMI);
	}

	// network

	public boolean hasWifi() {
		return isSet(FLAGS_WIFI);
	}

	// screen boolean

	public boolean screenSupportsInverted() {
		return isSet(FLAGS_SCR_INV);
	}

	public boolean screenSupportsContrast() {
		return isSet(FLAGS_SCR_CON);
	}

	public boolean screenSupportsBrightness() {
		return isSet(FLAGS_SCR_BRI);
	}

	public boolean screenSupportsAmbience() {
		return isSet(FLAGS_SCR_AMB);
	}

	// methods

	public Builder builder() {
		return new Builder(this);
	}

	private boolean isSet(int flag) {
		return (flags & flag) != 0;
	}

}
