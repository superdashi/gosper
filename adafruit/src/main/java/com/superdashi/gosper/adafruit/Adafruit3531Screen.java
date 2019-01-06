package com.superdashi.gosper.adafruit;

/*
 * Derived from:
 * https://github.com/adafruit/Adafruit_Python_SSD1306/blob/master/Adafruit_SSD1306/SSD1306.py
 * Original copyright: 2014 Adafruit Industries
 * Original author: Tony DiCola
 */

import java.util.Arrays;
import java.util.Random;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.I2CDevice;
import com.diozero.util.RuntimeIOException;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.Bits;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.studio.Composition;
import com.superdashi.gosper.studio.Target;
import com.tomgibara.intgeom.IntDimensions;

//TODO implement inverting properly
//TODO check if contrast is brightness
class Adafruit3531Screen implements Screen {

	// constants

	private static final int SSD1306_I2C_ADDRESS = 0x3C; // 011110+SA0+RW - 0x3C or 0x3D
	private static final int SSD1306_SETCONTRAST = 0x81;
	private static final int SSD1306_DISPLAYALLON_RESUME = 0xA4;
	private static final int SSD1306_DISPLAYALLON = 0xA5;
	private static final int SSD1306_NORMALDISPLAY = 0xA6;
	private static final int SSD1306_INVERTDISPLAY = 0xA7;
	private static final int SSD1306_DISPLAYOFF = 0xAE;
	private static final int SSD1306_DISPLAYON = 0xAF;
	private static final int SSD1306_SETDISPLAYOFFSET = 0xD3;
	private static final int SSD1306_SETCOMPINS = 0xDA;
	private static final int SSD1306_SETVCOMDETECT = 0xDB;
	private static final int SSD1306_SETDISPLAYCLOCKDIV = 0xD5;
	private static final int SSD1306_SETPRECHARGE = 0xD9;
	private static final int SSD1306_SETMULTIPLEX = 0xA8;
	private static final int SSD1306_SETLOWCOLUMN = 0x00;
	private static final int SSD1306_SETHIGHCOLUMN = 0x10;
	private static final int SSD1306_SETSTARTLINE = 0x40;
	private static final int SSD1306_MEMORYMODE = 0x20;
	private static final int SSD1306_COLUMNADDR = 0x21;
	private static final int SSD1306_PAGEADDR = 0x22;
	private static final int SSD1306_COMSCANINC = 0xC0;
	private static final int SSD1306_COMSCANDEC = 0xC8;
	private static final int SSD1306_SEGREMAP = 0xA0;
	private static final int SSD1306_CHARGEPUMP = 0x8D;
	private static final int SSD1306_EXTERNALVCC = 0x1;
	private static final int SSD1306_SWITCHCAPVCC = 0x2;

	// scrolling constants

	private static final int SSD1306_ACTIVATE_SCROLL = 0x2F;
	private static final int SSD1306_DEACTIVATE_SCROLL = 0x2E;
	private static final int SSD1306_SET_VERTICAL_SCROLL_AREA = 0xA3;
	private static final int SSD1306_RIGHT_HORIZONTAL_SCROLL = 0x26;
	private static final int SSD1306_LEFT_HORIZONTAL_SCROLL = 0x27;
	private static final int SSD1306_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = 0x29;
	private static final int SSD1306_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL = 0x2A;


	private enum Buffering {
		A_FROM_B,
		B_FROM_A,
		A_FROM_NIL;

		Buffering next() {
			return this == B_FROM_A ? A_FROM_B : B_FROM_A;
		}
	}

	private static final int width = 128;
	private static final int height = 64;
	private static final IntDimensions dimensions = IntDimensions.of(128, 64);

	// fields

	private final BitStore dirty = Bits.store(64);
	private final byte[] bytes = new byte[16];

	private final Logger logger;
	private final DigitalOutputDevice resetPin;
	private final byte[] bitmap = new byte[width * height / 8];
	private final Target target = Target.toByteBitmap(dimensions, bitmap);
	private final int[] bufferA = new int[width * height / 32];
	private final int[] bufferB = new int[width * height / 32];
	private final I2CDevice device;
	private Buffering buffering = Buffering.A_FROM_NIL;
	private int vccState = SSD1306_SWITCHCAPVCC;
	//private int vccState = SSD1306_EXTERNALVCC;
	private float contrast = external() ? 0x9F/255f : 0xCF/255f;
	private boolean inverted;

	public Adafruit3531Screen(Logger logger, int i2cBus, DigitalOutputDevice resetPin) {
		if (logger == null) throw new IllegalArgumentException("null logger");
		this.logger = logger;
		this.resetPin = resetPin;

		int i2cAdr = SSD1306_I2C_ADDRESS;
		try {
			device = new I2CDevice(i2cBus, i2cAdr);
		} catch (RuntimeIOException e) {
			logger.error().message("Failed to create I2C device {} on bus {}").values(i2cAdr, i2cBus).log();
			throw e;
		}
	}

	// screen methods

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
		return inverted;
	}

	@Override
	public void inverted(boolean inverted) {
		this.inverted = inverted;
	}

	@Override
	public float contrast() {
		return contrast;
	}

	@Override
	public float brightness() {
		return 1f;
	}

	@Override
	public void brightness(float brightness) {
		// noop
	}

	@Override
	public int ambience() {
		return 0;
	}

	@Override
	public void ambience(int color) {
		// noop
	}

	@Override
	public void contrast(float contrast) {
		if (contrast < 0f) throw new IllegalArgumentException("negative contrast");
		if (contrast > 1f) throw new IllegalArgumentException("invalid contrast");
		issue(SSD1306_SETCONTRAST);
		issue(Math.round(contrast * 255));
		this.contrast = contrast;
	}

	@Override
	public void begin() {
		reset();
		init();
		issue(SSD1306_DISPLAYON);
	}

	@Override
	public void end() {
		issue(SSD1306_DISPLAYOFF);
	}

	@Override
	public void reset() {
		if (resetPin == null) return;
		debug("resetting screen");
		resetPin.on();
		sleep(1L);
		resetPin.off();
		sleep(10L);
		resetPin.on();
		debug("screen reset");
	}

	@Override
	public void clear() {
		Arrays.fill(buffer(), (byte) 0);
	}

	@Override
	public void composite(Composition composition) {
		if (composition == null) throw new IllegalArgumentException("null composition");
		composition.compositeTo(target);
		int[] buffer = buffer();
		int j = 0;
		for (int x = 0; x < width; x ++) {
			for (int y = 0; y < height; y += 32) {
				buffer[j++] = readBits(x, y);
			}
		}
	}

	@Override
	public void update() {
		debug("updating screen");
		if (buffering == Buffering.A_FROM_NIL) {
			debug("no dirty, first update");
			transmitAll();
		} else {
			markDirty();
			long l = dirty.asNumber().longValue();
			if (l == 0) {
				/* nothing to do */
				debug("no dirty");
			} else if (l == -1) {
				/* everything is dirty */
				debug("all dirty");
				transmitAll();
			} else {
				BitMatches ones = dirty.ones();
				int outerFrom = ones.first();
				int outerTo = ones.last() + 1;
				if (ones.count() == outerTo - outerFrom) {
					/* contiguous dirty block */
					debug("contiguous dirty");
					transmit(outerFrom, outerTo);
				} else {
					/* transmit dirty columns segments separately */
					debug("split dirty");
					int from = -1;
					for (int i = outerFrom; i < outerTo; i++) {
						boolean next = dirty.getBit(i);
						if (next == (from >= 0)) continue;
						if (next) {
							from = i;
						} else {
							transmit(from, i);
							from = -1;
						}
					}
					transmit(from, outerTo);
				}
			}
		}
		buffering = buffering.next();
		debug("screen updated");
	}

	@Override
	public void blank() {
		Arrays.fill(buffer(), (byte) (inverted ? -1 : 0));
		update();
	}

	// additional methods

	public void noise(Random r) {
		int[] buffer = buffer();
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = r.nextInt();
		}
	}

	private boolean external() {
		return vccState == SSD1306_EXTERNALVCC;
	}

	private void init() {
		debug("initializing screen");
		boolean external = external();

		issue(SSD1306_DISPLAYOFF);                   // 0xAE
		issue(SSD1306_SETDISPLAYCLOCKDIV);           // 0xD5
		issue(0x80);                                 // the suggested ratio 0x80
		issue(SSD1306_SETMULTIPLEX);                 // 0xA8
		issue(0x3F);
		issue(SSD1306_SETDISPLAYOFFSET);             // 0xD3
		issue(0x0);                                  // no offset
		issue(SSD1306_SETSTARTLINE | 0x0);           // line # 0
		issue(SSD1306_CHARGEPUMP);                   // 0x8D
		if (external) {
			issue(0x10);
		} else {
			issue(0x14);
		}
		issue(SSD1306_MEMORYMODE);                   // 0x20
		issue(0x01);                                 // 0x01 vertical addressing mode
		issue(SSD1306_SEGREMAP | 0x1);
		issue(SSD1306_COMSCANDEC);
		issue(SSD1306_SETCOMPINS);                   // 0xDA
		issue(0x12);
		issue(SSD1306_SETCONTRAST);                  // 0x81
		issue(Math.round(contrast * 255));
		issue(SSD1306_SETPRECHARGE);                 // 0xd9
		if (external) {
			issue(0x22);
		} else {
			issue(0xF1);
		}
		issue(SSD1306_SETVCOMDETECT);                //  0xDB
		issue(0x40);
		issue(SSD1306_DISPLAYALLON_RESUME);          //  0xA4
		issue(SSD1306_NORMALDISPLAY);                //  0xA6
		debug("screen initialized");
	}

	private void issue(int command) {
		try {
			device.writeByte(0, command);
		} catch (RuntimeIOException e) {
			throw new RuntimeException("Failed to issue command to screen ", e);
		}
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			debug("sleep interrupted");
		}
	}

	private int[] buffer() {
		return buffering == Buffering.B_FROM_A ? bufferB : bufferA;
	}

	private int[] memory() {
		return buffering == Buffering.B_FROM_A ? bufferA : bufferB;
	}

	private void fillBytes(int tl, int bl, int tr, int br) {
		bytes[ 0] = (byte)  tl       ;
		bytes[ 1] = (byte) (tl >>  8);
		bytes[ 2] = (byte) (tl >> 16);
		bytes[ 3] = (byte) (tl >> 24);
		bytes[ 4] = (byte)  bl       ;
		bytes[ 5] = (byte) (bl >>  8);
		bytes[ 6] = (byte) (bl >> 16);
		bytes[ 7] = (byte) (bl >> 24);
		bytes[ 8] = (byte)  tr       ;
		bytes[ 9] = (byte) (tr >>  8);
		bytes[10] = (byte) (tr >> 16);
		bytes[11] = (byte) (tr >> 24);
		bytes[12] = (byte)  br       ;
		bytes[13] = (byte) (br >>  8);
		bytes[14] = (byte) (br >> 16);
		bytes[15] = (byte) (br >> 24);
	}

	private int readBits(int x, int y) {
		int bits = 0;
		int i = y * width + x; // index of bit
		int b = i / 8; // index of byte
		int s = 7 - (i % 8); // shift required
		int w = width / 8; // bytes per width (= 16)
		for (int j = b + w * 31; j >= b; j -= w) {
			bits <<= 1;
			bits |= (bitmap[j] >> s) & 1;
		}
		return bits;
	}

	private void markDirty() {
		int[] b = buffer();
		int[] m = memory();
		for (int i = 0; i < width / 2; i += 4) {
			dirty.setBit(i,
					b[i    ] != m[i    ] ||
					b[i + 1] != m[i + 1] ||
					b[i + 2] != m[i + 2] ||
					b[i + 3] != m[i + 3]
					);
		}
		dirty.setAll(true);
	}

	private void transmitAll() {
		transmit(0, width / 2);

	}

	//NOTE TO/FROM are column pairs
	private void transmit(int fromCol, int toCol) {
		debug("transmitting screen");
		issue(SSD1306_COLUMNADDR);
		issue(2 * fromCol);          // Column start address. (0 = reset)
		issue(2 * toCol-1);          // Column end address.
		issue(SSD1306_PAGEADDR);
		issue(0);                    // Page start address. (0 = reset)
		issue(height/8-1);           // Page end address.
		int control = 0x40;
		int[] buffer = buffer();
		try {
			if (inverted) {
				// two column strides
				for (int i = fromCol; i < toCol; i ++) {
					int j = i << 2;
					fillBytes(
							~buffer[j    ],
							~buffer[j + 1],
							~buffer[j + 2],
							~buffer[j + 3]
							);
					device.writeBytes(control, bytes.length, bytes);
				}
			} else {
				// two column strides
				for (int i = fromCol; i < toCol; i ++) {
					int j = i << 2;
					fillBytes(
							buffer[j    ],
							buffer[j + 1],
							buffer[j + 2],
							buffer[j + 3]
							);
					device.writeBytes(control, bytes.length, bytes);
				}
			}
		} catch (RuntimeIOException e) {
			throw new RuntimeException("Failed to write to screen");
		}
		debug("screen transmitted");
	}

	private void debug(String message) {
		logger.debug().message(message).log();
	}

}
