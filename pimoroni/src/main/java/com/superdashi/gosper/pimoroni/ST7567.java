package com.superdashi.gosper.pimoroni;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.SPIConstants;
import com.diozero.api.SpiClockMode;
import com.diozero.api.SpiDevice;

/*
 * Derived from: https://github.com/pimoroni/gfx-hat/blob/master/library/gfxhat/st7567.py
 * Original copyright: 2018 Pimoroni Ltd.
 */

public class ST7567 {

	static final int WIDTH = 128;
	static final int HEIGHT = 64;

	private static final int SPI_SPEED_HZ = 1000000;

	private static final int PIN_CS = 8;
	private static final int PIN_RST = 5;
	private static final int PIN_DC = 6;

	private static final int ST7567_PAGESIZE = 128;

	private static final byte ST7567_DISPOFF = (byte) 0xae;         /* 0xae: Display OFF (sleep mode) */
	private static final byte ST7567_DISPON = (byte) 0xaf;          /* 0xaf: Display ON in normal mode */

	private static final byte ST7567_SETSTARTLINE = (byte) 0x40;    /* 0x40-7f: Set display start line */
	private static final byte ST7567_STARTLINE_MASK =(byte)  0x3f;

	private static final byte ST7567_REG_RATIO = (byte) 0x20;

	private static final byte ST7567_SETPAGESTART = (byte) 0xb0;    /* 0xb0-b7: Set page start address */
	private static final byte ST7567_PAGESTART_MASK = (byte) 0x07;

	private static final byte ST7567_SETCOLL = (byte) 0x00;         /* 0x00-0x0f: Set lower column address */
	private static final byte ST7567_COLL_MASK = (byte) 0x0f;
	private static final byte ST7567_SETCOLH = (byte) 0x10;         /* 0x10-0x1f: Set higher column address */
	private static final byte ST7567_COLH_MASK = (byte) 0x0f;

	private static final byte ST7567_SEG_DIR_NORMAL = (byte) 0xa0;  /* 0xa0: Column address 0 is mapped to SEG0 */
	private static final byte ST7567_SEG_DIR_REV = (byte) 0xa1;     /* 0xa1: Column address 128 is mapped to SEG0 */

	private static final byte ST7567_DISPNORMAL = (byte) 0xa6;      /* 0xa6: Normal display */
	private static final byte ST7567_DISPINVERSE = (byte) 0xa7;     /* 0xa7: Inverse display */

	private static final byte ST7567_DISPRAM = (byte) 0xa4;         /* 0xa4: Resume to RAM content display */
	private static final byte ST7567_DISPENTIRE = (byte) 0xa5;      /* 0xa5: Entire display ON */

	private static final byte ST7567_BIAS_1_9 = (byte) 0xa2;        /* 0xa2: Select BIAS setting 1/9 */
	private static final byte ST7567_BIAS_1_7 = (byte) 0xa3;        /* 0xa3: Select BIAS setting 1/7 */

	private static final byte ST7567_ENTER_RMWMODE = (byte) 0xe0;   /* 0xe0: Enter the Read Modify Write mode */
	private static final byte ST7567_EXIT_RMWMODE = (byte) 0xee;    /* 0xee: Leave the Read Modify Write mode */
	private static final byte ST7567_EXIT_SOFTRST = (byte) 0xe2;    /* 0xe2: Software RESET */

	private static final byte ST7567_SETCOMNORMAL = (byte) 0xc0;    /* 0xc0: Set COM output direction, normal mode */
	private static final byte ST7567_SETCOMREVERSE = (byte) 0xc8;   /* 0xc8: Set COM output direction, reverse mode */

	private static final byte ST7567_POWERCTRL_VF = (byte) 0x29;    /* 0x29: Control built-in power circuit */
	private static final byte ST7567_POWERCTRL_VR = (byte) 0x2a;    /* 0x2a: Control built-in power circuit */
	private static final byte ST7567_POWERCTRL_VB = (byte) 0x2c;    /* 0x2c: Control built-in power circuit */
	private static final byte ST7567_POWERCTRL = (byte) 0x2f;       /* 0x2c: Control built-in power circuit */

	private static final byte ST7567_REG_RES_RR0 = (byte) 0x21;     /* 0x21: Regulation Resistior ratio */
	private static final byte ST7567_REG_RES_RR1 = (byte) 0x22;     /* 0x22: Regulation Resistior ratio */
	private static final byte ST7567_REG_RES_RR2 = (byte) 0x24;     /* 0x24: Regulation Resistior ratio */

	private static final byte ST7567_SETCONTRAST = (byte) 0x81;     /* 0x81: Set contrast control */

	private static final byte ST7567_SETBOOSTER = (byte) 0xf8;      /* Set booster level */
	private static final byte ST7567_SETBOOSTER4X = (byte) 0x00;    /* Set booster level */
	private static final byte ST7567_SETBOOSTER5X = (byte) 0x01;    /* Set booster level */

	private static final byte ST7567_NOP = (byte) 0xe3;             /* 0xe3: NOP Command for no operation */

	private static final int ST7565_STARTBYTES = 0;

	private final int rst;
	private final int dc;
	private final int spiBus;
	private final int spiCs;
	private final int spiSpeed;

	private DigitalOutputDevice pinRst;
	private DigitalOutputDevice pinDc;
	private SpiDevice spi;

	private boolean inverted = true;
	private byte contrast = 58;

	public ST7567() {
		this(PIN_RST, PIN_DC, 0, 0, SPI_SPEED_HZ);
	}

	public ST7567(int rst, int dc, int spiBus, int spiCs, int spiSpeed) {
		if (spiSpeed < 500000 || spiSpeed > 32000000) throw new IllegalArgumentException("invalid spiSpeed");
		this.rst = rst;
		this.dc = dc;
		this.spiBus = spiBus;
		this.spiCs = spiCs;
		this.spiSpeed = spiSpeed;
	}

	public void setUp() {
		if (spi != null) throw new IllegalStateException();
		spi = new SpiDevice(SPIConstants.DEFAULT_SPI_CONTROLLER, SPIConstants.CE0, spiSpeed, SpiClockMode.MODE_0, SPIConstants.DEFAULT_LSB_FIRST);
		pinRst = new DigitalOutputDevice(rst);
		pinDc = new DigitalOutputDevice(dc);

		reset();
		init();
	}

	public void tearDown() {
		if (spi == null) throw new IllegalStateException();
		spi.close();
		pinRst.close();
		pinDc.close();
	}

	public void reset() {
		pinRst.setOn(false);
		sleep(10L);
		pinRst.setOn(true);
		sleep(100L);
	}

	public boolean inverted() {
		return inverted;
	}

	public void inverted(boolean inverted) {
		this.inverted = inverted;
		if (spi != null) {
			command(inverted ? ST7567_DISPINVERSE : ST7567_DISPNORMAL);
		}
	}

	public float contrast() {
		return (contrast & 0xff) / 63f;
	}

	public void constrast(float contrast) {
		contrast = Math.max(Math.min(contrast, 1f), 0f);
		this.contrast = (byte) (contrast * 63);
		if (spi != null) {
			command(ST7567_SETCONTRAST, this.contrast);
		}
	}
	public void update(byte[] buffer) {
		if (spi == null) throw new IllegalStateException();
		command(ST7567_ENTER_RMWMODE);
		byte[] triple = { ST7567_SETPAGESTART, ST7567_SETCOLL, ST7567_SETCOLH };
		for (int page = 0; page < 8; page++) {
			triple[0] = (byte) (ST7567_SETPAGESTART | page);
			command(triple);
			data(buffer, page * ST7567_PAGESIZE, ST7567_PAGESIZE);
		}
		command(ST7567_EXIT_RMWMODE);
	}

//	public void setPixel(int x, int y, boolean b) {
//		int offset = (y >> 3) * WIDTH + x;
//		int mask = 1 << (y & 0x7);
//		int value = buffer[offset];
//		value = b ? value | mask : value & ~mask;
//		buffer[offset] = (byte) value;
//	}

	private void command(byte... bytes) {
		command(bytes, 0, bytes.length);
	}

	private void command(byte[] bytes, int offset, int length) {
		pinDc.setOn(false);
		spi.write(bytes, offset, length);
	}

	private void data(byte[] bytes, int offset, int length) {
		pinDc.setOn(true);
		spi.write(bytes, offset, length);
	}

	private void init() {
		command(new byte[] {
			ST7567_BIAS_1_7,          // Bias 1/7 (0xA2 = Bias 1/9)
			ST7567_SEG_DIR_NORMAL,
			ST7567_SETCOMREVERSE,     // Reverse COM - vertical flip
			inverted ? ST7567_DISPINVERSE : ST7567_DISPNORMAL,        // Inverse display (0xA6 normal)
			ST7567_SETSTARTLINE | 0,  // Start at line 0
			ST7567_POWERCTRL,
			ST7567_REG_RATIO | 2,
			ST7567_DISPON,
			ST7567_SETCONTRAST,       // Set contrast
			contrast                        // Contrast value
		});
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			//TODO log
		}
	}
}
