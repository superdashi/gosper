package com.superdashi.gosper.linux;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Function;

import com.superdashi.gosper.device.DeviceException;
import com.superdashi.gosper.device.Event;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.Streams;

//NOTE: currently only handles events required for rasp pi touch screens
//TODO: need to handle multi-touch properly
public final class LinuxEvents {

	private static final int TYPE_SYN = 0;
	private static final int TYPE_KEY = 1;
	private static final int TYPE_ABS = 3;

	private static final int CODE_BTN_TOUCH = 330;
	private static final int CODE_ABS_X = 0;
	private static final int CODE_ABS_Y = 1;
	private static final int CODE_MT_SLOT = 47;
	private static final int CODE_MT_POSITION_X = 53;
	private static final int CODE_MT_POSITION_Y = 54;
	private static final int CODE_MT_TRACKING_ID = 57;

	private final Path devicePath;
	private final int pointLimitSqr;
	private final Function<IntCoords, IntCoords> transform;

	private FileChannel channel = null;
	private EventThread thread = null;

	// eg
	public LinuxEvents(String device, float pointLimit, Function<IntCoords, IntCoords> transform) {
		if (device == null) throw new IllegalArgumentException("null device");
		if (device.isEmpty()) throw new IllegalArgumentException("empty device");
		devicePath = Paths.get(device);
		if (pointLimit < 0f) throw new IllegalArgumentException("negative pointLimit");
		if (transform == null) throw new IllegalArgumentException("null transform");
		pointLimitSqr = Math.round(pointLimit * pointLimit);
		this.transform = transform;
	}

	public void start(Consumer<Event> consumer) throws DeviceException {
		if (consumer == null) throw new IllegalArgumentException("null consumer");
		if (started()) throw new IllegalStateException("started");
		EnumSet<StandardOpenOption> options = EnumSet.of(StandardOpenOption.READ);
		try {
			channel = FileChannel.open(devicePath, options);
		} catch (IOException e) {
			throw new DeviceException("failed to open event device " + devicePath, e);
		}
		thread = new EventThread(consumer);
		thread.start();
	}

	public void stop(long timeout) throws DeviceException {
		if (timeout < 0L) throw new IllegalArgumentException("negative timeout");
		if (!started()) throw new IllegalStateException("not started");
		try {
			thread.interrupt();
			thread.join(timeout);
		} catch (InterruptedException e) {
			throw new DeviceException("interrupted waiting for event thread to die", e);
		} finally {
			thread = null;
			try {
				channel.close();
			} catch (IOException e) {
				//TODO log
				e.printStackTrace();
			} finally {
				channel = null;
			}
		}
	}

	public boolean started() {
		return thread != null;
	}

	private class EventThread extends Thread {

		private final Consumer<Event> consumer;
		private final boolean sfb = LinuxUtil.is64Bit();

		EventThread(Consumer<Event> consumer) {
			super("linux-event");
			this.consumer = consumer;
		}

		@Override
		public void run() {
			// book keeping fields
			boolean downChange = false;
			int lastX = Integer.MIN_VALUE;
			int lastY = Integer.MIN_VALUE;

			boolean maybePoint = false;
			long startT = -1L;
			int startX = 0;
			int startY = 0;

			// event fields
			int x = -1;
			int y = -1;
			boolean down = false;

			ByteBuffer buffer = ByteBuffer.allocateDirect(sfb ? 24 : 16);
			while (true) {
				while (buffer.hasRemaining()) {
					try {
						int r = channel.read(buffer);
						if (r < 0) throw new IOException("unexpected end of file");
					} catch (ClosedByInterruptException e) {
						// we're stopping
						return;
					} catch (IOException e) {
						//TODO log
						e.printStackTrace();
						return;
					}
				}
				((Buffer) buffer).flip();
				// we have a full buffer, now convert and deliver
				long stamp;
				int type;
				int code;
				int value;
				try (ReadStream in = Streams.streamBuffer(buffer).readStream()) {
					if (sfb) {
						long secs = Long.reverseBytes(in.readLong());
						long usecs = Long.reverseBytes(in.readLong());
						stamp = secs * 1000L + usecs / 1000L;
					} else {
						int secs = Integer.reverseBytes(in.readInt());
						int usecs = Integer.reverseBytes(in.readInt());
						stamp = secs * 1000L + usecs / 1000;
					}
					type = Short.reverseBytes(in.readShort());
					code = Short.reverseBytes(in.readShort());
					value = Integer.reverseBytes(in.readInt());
				}
				((Buffer) buffer).clear();
				//System.out.printf("FRAME RECEIVED %016x %04x %04x %08x%n", stamp, type, code, value);

				switch (type) {
				case TYPE_SYN:
					// first check if this could still be a pointing
					if (maybePoint) {
						int dSqr = (x-startX)*(x-startX) + (y-startY)*(y-startY);
						maybePoint = dSqr <= pointLimitSqr;
					}

					// now check if it's the start or end of a pointing
					if (downChange) {
						if (down) {
							// this could be the start of a click, so start monitoring
							maybePoint = true;
							startT = stamp;
							startX = x;
							startY = y;
						} else {
							if (maybePoint) {
								// this was a 'click', so send an extra event
								//TODO may want to support modifiers in the future
								IntCoords coords = transform(x, y);
								consumer.accept(Event.newPointEvent(Event.KEY_NONE, coords.x, coords.y, stamp));
							}
							// in any case, it can no longer be a point
							maybePoint = false;
						}
					}

					// deliver a move event if position has change
					if (downChange || x != lastX || y != lastY) {
						IntCoords coords = transform(x, y);
						consumer.accept(Event.newMoveEvent(Event.KEY_NONE, coords.x, coords.y, down, !downChange, stamp));
						lastX = x;
						lastY = y;
						downChange = false;
					}
					break;
				case TYPE_KEY:
					switch (code) {
					case CODE_BTN_TOUCH:
						downChange = true;
						down = value != 0;
						break;
					}
					break;
				case TYPE_ABS:
					switch(code) {
						case CODE_ABS_X:
							x = value;
							break;
						case CODE_ABS_Y:
							y = value;
							break;
						case CODE_MT_POSITION_X:
						case CODE_MT_POSITION_Y:
						case CODE_MT_SLOT:
						case CODE_MT_TRACKING_ID:
							//TODO currently multitouch is ignored
							break;
					}
					break;
				}
			}
		}

		private IntCoords transform(int x, int y) {
			return transform.apply(IntCoords.at(x, y));
		}
	}
}
