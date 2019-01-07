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
package com.superdashi.gosper.color;

public final class Argb {

	private static final int DARKER_SCALE = 179;

	public static final int WHITE   = 0xffffffff;
	public static final int BLACK   = 0xff000000;
	public static final int RED     = 0xffff0000;
	public static final int GREEN   = 0xff00ff00;
	public static final int BLUE    = 0xff0000ff;
	public static final int GRAY    = 0xff808080;
	public static final int YELLOW  = 0xffffff00;
	public static final int CYAN    = 0xff00ffff;
	public static final int MAGENTA = 0xffff00ff;

	public static final int gray(int v) {
		if (v <   0) return 0xff000000;
		if (v > 255) return 0xffffffff;
		return (0x00010101 * v) | 0xff000000;
	}

	private static final int clamp(int ch) {
		return Math.min(0xff, Math.max(0, ch));
	}

	// components

	public static int alpha(int c) {
		return c >>> 24;
	}

	public static int red(int c) {
		return (c >> 16) & 0xff;
	}

	public static int green(int c) {
		return (c >> 8) & 0xff;
	}

	public static int blue(int c) {
		return c & 0xff;
	}

	public static String toString(int c) {
		return String.format("0x%08x", c);
	}

	// tests

	public static boolean isOpaque(int c) {
		return alpha(c) == 0xff;
	}

	public static boolean isTransparent(int c) {
		return alpha(c) == 0;
	}

	// components must be in range 0-255 incl.
	public static int argb(int a, int r, int g, int b) {
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// color adjustment

	//TODO support weighted intensity?
	public static int intensity(int color) {
		return (red(color) + green(color) + blue(color)) / 3;
	}

	public static int opaque(int argb) {
		return (argb & 0x00ffffff) | (0xff000000);
	}

	public static int transparent(int argb) {
		return argb & 0x00ffffff;
	}

	public static int setAlpha(int argb, int a) { return (argb & 0x00ffffff) | (clamp(a) << 24); }
	public static int setRed  (int argb, int r) { return (argb & 0xff00ffff) | (clamp(r) << 16); }
	public static int setGreen(int argb, int g) { return (argb & 0xffff00ff) | (clamp(g) <<  8); }
	public static int setBlue (int argb, int b) { return (argb & 0xffffff00) | (clamp(b)      ); }

	public static int setAlpha(int argb, float a) { return setAlpha(argb, Math.round(a * 255)); }
	public static int setRed  (int argb, float r) { return setRed  (argb, Math.round(r * 255)); }
	public static int setGreen(int argb, float g) { return setGreen(argb, Math.round(g * 255)); }
	public static int setBlue (int argb, float b) { return setBlue (argb, Math.round(b * 255)); }

	// leaves alpha unchanged
	public static int inverse(int argb) {
		return Argb.argb(
				      alpha(argb),
				255 - red  (argb),
				255 - green(argb),
				255 - blue (argb)
				);
	}

	public static int darker(int argb) {
		return Argb.argb(
				 Argb.alpha(argb),
				(Argb.red  (argb) * DARKER_SCALE) >> 8,
				(Argb.green(argb) * DARKER_SCALE) >> 8,
				(Argb.blue (argb) * DARKER_SCALE) >> 8
				);
	}

	//NOTE could optimize
	public static int lighter(int argb) {
		return Argb.inverse( darker( Argb.inverse(argb) ) );
	}

	// blending

	public static int mix(int argb1, int argb2, float s) {
		if (argb1 == argb2) return argb1;
		if (s <= 0f) return argb1;
		if (s >= 1f) return argb2;
		float t = 1f - s;
		float a = s * alpha(argb2) + t * alpha(argb1);
		float r = s * red(argb2)   + t * red(argb1)  ;
		float g = s * green(argb2) + t * green(argb1);
		float b = s * blue(argb2)  + t * blue(argb1) ;
		return argb((int) a, (int) r, (int) g, (int) b);
	}

	public static int premultiply(int argb) {
		int a = Argb.alpha(argb);
		switch (a) {
		case 0x00 : return 0xff000000;
		case 0xff : return argb;
		default:
			int r = Argb.red  (argb) * a / 255;
			int g = Argb.green(argb) * a / 255;
			int b = Argb.blue (argb) * a / 255;
			return argb(0xff, r, g, b);
		}
	}

	// porter duff

//	public static int srcOver(int argbSrc, int argbDst) {
//		int srcOver = srcOverImpl(argbSrc, argbDst);
//		System.out.printf("%08x %08x -> %08x%n", argbSrc, argbDst, srcOver);
//		return srcOver;
//	}

	public static int srcOver(int argbSrc, int argbDst) {
		int aSrc = alpha(argbSrc);
		if (aSrc == 0) return argbDst;
		if (aSrc == 255) return argbSrc;
		int aDst = alpha(argbDst);
		int s = aSrc;
		int t = (255 - s) * aDst;

		return argb(
			s + t / 255,
			(s * red  (argbSrc) + t * red  (argbDst)) / 255,
			(s * green(argbSrc) + t * green(argbDst)) / 255,
			(s * blue (argbSrc) + t * blue (argbDst)) / 255
			);
	}

	private Argb() { }

}
