/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.superdashi.gosper.pimoroni;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.device.DeviceException;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.pimoroni.PimoroniGfxHat.Led;
import com.superdashi.gosper.studio.Composition;
import com.superdashi.gosper.studio.Target;
import com.tomgibara.intgeom.IntDimensions;

//TODO log all calls
class PimoroniGfxHatScreen implements Screen {

	// statics

	private static final IntDimensions dimensions = IntDimensions.of(ST7567.WIDTH, ST7567.HEIGHT);

	public static Set<Led> leds(Led... leds) {
		if (leds == null) throw new IllegalArgumentException("null leds");
		//TODO test if leds contains null?
		switch (leds.length) {
		case 0 : return EnumSet.noneOf(Led.class);
		case 1 : return EnumSet.of(leds[0]);
		default : return EnumSet.copyOf(Arrays.asList(leds));
		}
	}

	public static LedColors createLedColors() {
		return new LedColors();
	}

	private final Logger logger;
	private final ST7567 st7567;
	private final SN3218 sn3218;
	private final byte[] bitmap = new byte[dimensions.area() / 8];
	private final byte[] buffer = new byte[dimensions.area() / 8];
	private final Target target = Target.toByteBitmap(dimensions, bitmap);
	private final Object bufferLock = new Object();

	private float brightness = 0f;
	private int leftColor = Argb.BLACK;
	private int rightColor = Argb.BLACK;

	PimoroniGfxHatScreen(Logger logger, ST7567 st7567, SN3218 sn3218) {
		this.logger = logger;
		this.st7567 = st7567;
		this.sn3218 = sn3218;
	}

	@Override
	public IntDimensions dimensions() {
		return dimensions;
	}

	@Override
	public boolean opaque() {
		return true;
	}

	@Override
	public boolean inverted() {
		return !st7567.inverted();
	}

	@Override
	public void inverted(boolean inverted) {
		st7567.inverted(!inverted);
	}

	@Override
	public float contrast() {
		return st7567.contrast();
	}

	@Override
	public void contrast(float contrast) {
		st7567.constrast(contrast);
	}

	@Override
	public float brightness() {
		return brightness;
	}

	@Override
	public void brightness(float brightness) {
		update(brightness, leftColor, rightColor);
	}

	@Override
	public int ambience() {
		return Argb.mix(leftColor, rightColor, 0.5f);
	}

	@Override
	public void ambience(int color) {
		update(brightness, color, color);
	}

	@Override
	public void begin() throws DeviceException {
		debugLog("beginning");
		sn3218.setUp();
		sn3218.enable();
		update(0.8f, Argb.WHITE, Argb.WHITE);
		st7567.setUp();
		debugLog("begun");
	}

	@Override
	public void end() throws DeviceException {
		debugLog("ending");
		st7567.tearDown();
		sn3218.disable();
		sn3218.tearDown();
		debugLog("ended");
	}

	@Override
	public void reset() throws DeviceException {
		debugLog("resetting");
		st7567.reset();
		debugLog("reset");
	}

	@Override
	public void clear() {
		debugLog("clearing");
		synchronized (bufferLock) {
			Arrays.fill(buffer, (byte) 0);
		}
		debugLog("cleared");
	}

	@Override
	public void composite(Composition composition) {
		debugLog("compositing");
		composition.compositeTo(target);
		synchronized (bufferLock) {
			for (int ty = 0; ty < 8; ty++) {
				for (int tx = 0; tx < 16; tx++) {
					int i = tx + (ty * 8) * 16;
					byte b0 = bitmap[i      ];
					byte b1 = bitmap[i +  16];
					byte b2 = bitmap[i +  32];
					byte b3 = bitmap[i +  48];
					byte b4 = bitmap[i +  64];
					byte b5 = bitmap[i +  80];
					byte b6 = bitmap[i +  96];
					byte b7 = bitmap[i + 112];
					byte c0 = (byte) (((b0 & 0x80) >> 7) | ((b1 & 0x80) >> 6) | ((b2 & 0x80) >> 5) | ((b3 & 0x80) >> 4) | ((b4 & 0x80) >> 3) | ((b5 & 0x80) >> 2) | ((b6 & 0x80) >> 1) | ((b7 & 0x80)     ) );
					byte c1 = (byte) (((b0 & 0x40) >> 6) | ((b1 & 0x40) >> 5) | ((b2 & 0x40) >> 4) | ((b3 & 0x40) >> 3) | ((b4 & 0x40) >> 2) | ((b5 & 0x40) >> 1) | ((b6 & 0x40)     ) | ((b7 & 0x40) << 1) );
					byte c2 = (byte) (((b0 & 0x20) >> 5) | ((b1 & 0x20) >> 4) | ((b2 & 0x20) >> 3) | ((b3 & 0x20) >> 2) | ((b4 & 0x20) >> 1) | ((b5 & 0x20)     ) | ((b6 & 0x20) << 1) | ((b7 & 0x20) << 2) );
					byte c3 = (byte) (((b0 & 0x10) >> 4) | ((b1 & 0x10) >> 3) | ((b2 & 0x10) >> 2) | ((b3 & 0x10) >> 1) | ((b4 & 0x10)     ) | ((b5 & 0x10) << 1) | ((b6 & 0x10) << 2) | ((b7 & 0x10) << 3) );
					byte c4 = (byte) (((b0 & 0x08) >> 3) | ((b1 & 0x08) >> 2) | ((b2 & 0x08) >> 1) | ((b3 & 0x08)     ) | ((b4 & 0x08) << 1) | ((b5 & 0x08) << 2) | ((b6 & 0x08) << 3) | ((b7 & 0x08) << 4) );
					byte c5 = (byte) (((b0 & 0x04) >> 2) | ((b1 & 0x04) >> 1) | ((b2 & 0x04)     ) | ((b3 & 0x04) << 1) | ((b4 & 0x04) << 2) | ((b5 & 0x04) << 3) | ((b6 & 0x04) << 4) | ((b7 & 0x04) << 5) );
					byte c6 = (byte) (((b0 & 0x02) >> 1) | ((b1 & 0x02)     ) | ((b2 & 0x02) << 1) | ((b3 & 0x02) << 2) | ((b4 & 0x02) << 3) | ((b5 & 0x02) << 4) | ((b6 & 0x02) << 5) | ((b7 & 0x02) << 6) );
					byte c7 = (byte) (((b0 & 0x01)     ) | ((b1 & 0x01) << 1) | ((b2 & 0x01) << 2) | ((b3 & 0x01) << 3) | ((b4 & 0x01) << 4) | ((b5 & 0x01) << 5) | ((b6 & 0x01) << 6) | ((b7 & 0x01) << 7) );
					int j = (tx * 8) + (ty * 8) * 16;
					buffer[j    ] = c0;
					buffer[j + 1] = c1;
					buffer[j + 2] = c2;
					buffer[j + 3] = c3;
					buffer[j + 4] = c4;
					buffer[j + 5] = c5;
					buffer[j + 6] = c6;
					buffer[j + 7] = c7;
				}
			}
		}
		debugLog("composited");
	}

	@Override
	public void blank() {
		debugLog("blanking");
		synchronized (bufferLock) {
			Arrays.fill(buffer, (byte) 0);
			st7567.update(buffer);
		}
		debugLog("blanked");
	}

	@Override
	public void update() {
		debugLog("updating");
		synchronized (bufferLock) {
			st7567.update(buffer);
		}
		debugLog("updated");
	}

	public void enableLeds(Set<Led> leds) {
		if (leds == null) throw new IllegalArgumentException("null leds");
		checkInitialized();
		sn3218.enableLeds(mask(leds));
	}

	public void enableAllLeds() {
		checkInitialized();
		sn3218.enableAllLeds();
	}

	public void disableLeds(Set<Led> leds) {
		if (leds == null) throw new IllegalArgumentException("null leds");
		checkInitialized();
		sn3218.enableLeds(~mask(leds));
	}

	public void disableAllLeds() {
		checkInitialized();
		sn3218.disableAllLeds();
	}

	public void ledColors(LedColors colors) {
		if (colors == null) throw new IllegalArgumentException("null colors");
		checkInitialized();
		sn3218.output(colors.values());
	}

	private void update(float brightness, int leftColor, int rightColor) {
		if (brightness == this.brightness && leftColor == this.leftColor && rightColor == this.rightColor) return; // nothing to do
		if (brightness == 0f && this.brightness != 0f) { // disable LEDs
			disableAllLeds();
		} else if (brightness != 0f && this.brightness == 0f) {
			enableAllLeds();
		}

		if (brightness != 0f) {
			int left  = Argb.setAlpha(leftColor , brightness);
			int right = Argb.setAlpha(rightColor, brightness);
			left  = Argb.premultiply(left );
			right = Argb.premultiply(right);
			LedColors colors = createLedColors();
			float d = Led.values().length - 1f;
			for (Led led : Led.values()) {
				int c = Argb.mix(left, right, led.ordinal() / d);
				colors.setColor(led, c);
			}
			ledColors(colors);
		}

		this.brightness = brightness;
		this.leftColor = leftColor;
		this.rightColor = rightColor;
	}

	private int mask(Set<Led> leds) {
		int mask = 0;
		for (Led led : leds) {
			mask |= 7 << (led.index * 3);
		}
		return mask;
	}

	private void checkInitialized() {
		//TODO no-op atm
	}

	private void debugLog(String msg) {
		logger.debug().message(msg).log();
	}

	public static final class LedColors {

		private final int[] colors = new int[6];

		LedColors() { }

		public void setColor(Led led, int color) {
			if (led == null) throw new IllegalArgumentException("null led");
			colors[led.index] = color;
		}

		public void setAll(int color) {
			Arrays.fill(colors, color);
		}

		public int getColor(Led led) {
			if (led == null) throw new IllegalArgumentException("null led");
			return colors[led.index];
		}

		public int[] getAll() {
			return colors.clone();
		}

		byte[] values() {
			byte[] values = new byte[18];
			for (int i = 0; i < 6; i++) {
				int c = colors[i];
				int j = i * 3;
				values[j + 2] = (byte) (c >> 16);
				values[j + 1] = (byte) (c >>  8);
				values[j + 0] = (byte)  c       ;
			}
			return values;
		}
	}

}
