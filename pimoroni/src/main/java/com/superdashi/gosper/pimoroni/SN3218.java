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

import com.diozero.api.I2CDevice;

public class SN3218 {

	private static final int I2C_ADDRESS = 0x54;
	private static final int CMD_ENABLE_OUTPUT = 0x00;
	private static final int CMD_SET_PWM_VALUES = 0x01;
	private static final int CMD_ENABLE_LEDS = 0x13;
	private static final int CMD_UPDATE = 0x16;
	private static final int CMD_RESET = 0x17;

	private static final byte[] DEFAULT_GAMMA = new byte[256];
	static {
		for (int i = 0; i < 256; i++) {
			DEFAULT_GAMMA[i] = (byte) (Math.pow(256.0, i/255.0) - 1);
		}
	}

	private final byte[] gamma = DEFAULT_GAMMA.clone();

	private I2CDevice i2c = null;

	public void setUp() {
		//TODO controller here should be configurable
		i2c = new I2CDevice(1, I2C_ADDRESS);
	}

	public void enable() {
		i2c.writeByte(CMD_ENABLE_OUTPUT, 0x01);
	}

	public void disable() {
		i2c.writeByte(CMD_ENABLE_OUTPUT, 0x00);
	}

	public void reset() {
		i2c.writeByte(CMD_RESET, 0xff);
	}

	public void enableLeds(int mask) {
		i2c.writeBytes(CMD_ENABLE_LEDS, 3, new byte[] {
			(byte) ( mask        & 0x3f),
			(byte) ((mask >>  6) & 0x3f),
			(byte) ((mask >> 12) & 0x3f)
		});
		i2c.writeByte(CMD_UPDATE, 0xff);
	}

	public void enableAllLeds() {
		enableLeds(0b111111111111111111);
	}

	public void disableLeds(int mask) {
		enableLeds(~mask);
	}

	public void disableAllLeds() {
		enableLeds(0);
	}

	public void output(byte[] values) {
		if (values == null) throw new IllegalArgumentException("null values");
		if (values.length != 18) throw new IllegalArgumentException("values not length 18");
		byte[] bytes = new byte[18];
		for (int i = 0; i < values.length; i++) {
			bytes[i] = gamma[values[i] & 0xff];
		}
		i2c.writeBytes(CMD_SET_PWM_VALUES, 18, bytes);
		i2c.writeByte(CMD_UPDATE, 0xff);
	}

	public void tearDown() {
		i2c.close();
	}

	static void dummy() {
		System.exit(0);
	}
}
