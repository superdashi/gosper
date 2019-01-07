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
package com.superdashi.gosper.adafruit;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import com.diozero.api.PwmOutputDevice;
import com.superdashi.gosper.device.DeviceException;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.studio.Composition;
import com.superdashi.gosper.studio.Target;
import com.tomgibara.intgeom.IntDimensions;

class Adafruit2423Screen implements Screen {

	private static final int width = 320;
	private static final int height = 240;
	private static final int size = width * height;
	private static final IntDimensions dimensions = IntDimensions.of(width, height);

	private final short[] surface = new short[size];
	private final Target target = Target.toShort565ARGB(dimensions, surface);
	private final byte[] buffer = new byte[size * 2];
	private final String devicePath;
	private final PwmOutputDevice briPin;
	private final Logger logger;

	// allocated on begin
	private RandomAccessFile raf;
	//private FileChannel channel;
	//private MappedByteBuffer mmap;

	Adafruit2423Screen(String devicePath, PwmOutputDevice briPin, Logger logger) {
		this.devicePath = devicePath;
		this.briPin = briPin;
		this.logger = logger;
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
		return false;
	}

	@Override
	public void inverted(boolean inverted) {
		// ignored
	}

	@Override
	public float contrast() {
		return 1f;
	}

	@Override
	public void contrast(float contrast) {
		// ignored
	}

	@Override
	public float brightness() {
		return briPin == null ? Float.NaN : briPin.getValue();
	}

	@Override
	public void brightness(float brightness) {
		if (brightness < 0f) throw new IllegalArgumentException("negative brightness");
		if (brightness > 1f) throw new IllegalArgumentException("invalid brightness");
		if (briPin == null) return;
		briPin.setValue(brightness);
	}

	@Override
	public int ambience() {
		return 0;
	}

	@Override
	public void ambience(int color) {
		// ignored
	}

	@Override
	@SuppressWarnings("resource")
	public void begin() {
		if (raf != null) throw new IllegalStateException("already begun");
		try {
			raf = new RandomAccessFile(devicePath, "rw");
			//mmap = channel.map(MapMode.READ_WRITE, 0, size * 2);
		} catch (IOException e) {
			logger.error().message("failed to open framebuffer device {}").values(devicePath).stacktrace(e).log();
			throw new DeviceException("failed to open framebuffer");
		}
	}

	@Override
	public void end() {
		if (raf == null) throw new IllegalStateException("not begun");
		try {
			raf.close();
		} catch (IOException e) {
			logger.error().message("failed to close framebuffer device {}").values(devicePath).stacktrace(e).log();
		} finally {
//			channel = null;
//			mmap = null;
			raf = null;
		}
	}

	@Override
	public void reset() {
		/* noop */
	}

	@Override
	public void clear() {
		Arrays.fill(buffer, (byte) 0);
	}

	@Override
	public void composite(Composition composition) {
		if (composition == null) throw new IllegalArgumentException("null composition");
		composition.compositeTo(target);
		//TODO irritating that this is necessary
		for (int i = 0; i < surface.length; i++) {
			short s = surface[i];
			buffer[i * 2    ] = (byte)  s      ;
			buffer[i * 2 + 1] = (byte) (s >> 8);
		}

	}

	@Override
	public void blank() {
		clear();
		update();
	}

	@Override
	public void update() {
//		mmap.clear();
//		mmap.put(buffer);
		try {
			raf.seek(0L);
			raf.write(buffer);
		} catch (IOException e) {
			throw new DeviceException("failed to update framebuffer", e);
		}
	}

}
