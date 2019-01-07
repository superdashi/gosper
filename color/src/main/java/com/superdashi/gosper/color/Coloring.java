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

import static com.superdashi.gosper.color.Argb.BLACK;
import static com.superdashi.gosper.color.Argb.WHITE;
import static com.superdashi.gosper.color.Argb.alpha;
import static com.superdashi.gosper.color.Argb.argb;
import static com.superdashi.gosper.color.Argb.blue;
import static com.superdashi.gosper.color.Argb.green;
import static com.superdashi.gosper.color.Argb.mix;
import static com.superdashi.gosper.color.Argb.red;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Coloring {

	private static final float[] lookup = new float[256];
	static {
		float s = 1f / 255;
		for (int i = 1; i < 255; i++) {
			lookup[i] = i * s;
		}
		lookup[255] = 1f;
	}

	public enum Corner {

		BL,
		BR,
		TL,
		TR

	}

	// components

	public static float asFloat(int c) {
		return lookup[c];
	}

	public static int asInt(float c) {
		c *= 255f;
		int i = Math.round(c);
		if (i < 0) return 0;
		if (i > 255) return 255;
		return i;
	}

	// colors

	public static final Coloring NONE = new Coloring(0,0,0,0);

	public static float[] colorAsFloats(int c) {
		float[] fs = new float[4];
		writeColorAsFloats(c, fs, 0);
		return fs;
	}

	public static int argbToRGBA(int argb) {
		return (argb >>> 24) | (argb << 8);
	}

	public static void writeColorAsFloats(int c, float[] fs, int i) {
		fs[i    ] = asFloat(red  (c));
		fs[i + 1] = asFloat(green(c));
		fs[i + 2] = asFloat(blue (c));
		fs[i + 3] = asFloat(alpha(c));
	}

	public static void writeOpaqueColorAsFloats(int c, float[] fs, int i) {
		fs[i    ] = asFloat(red  (c));
		fs[i + 1] = asFloat(green(c));
		fs[i + 2] = asFloat(blue (c));
	}

	public static int readColorAsFloats(float[] fs, int i) {
		int r = asInt(fs[i    ]);
		int g = asInt(fs[i + 1]);
		int b = asInt(fs[i + 2]);
		int a = asInt(fs[i + 3]);
		return argb(a,r,g,b);
	}

	public static int readOpaqueColorAsFloats(float[] fs, int i) {
		int r = asInt(fs[i    ]);
		int g = asInt(fs[i + 1]);
		int b = asInt(fs[i + 2]);
		return argb(0xff,r,g,b);
	}

	public static void writeColor(int c, FloatBuffer b) {
		b.put( asFloat(red  (c)) );
		b.put( asFloat(green(c)) );
		b.put( asFloat(blue (c)) );
		b.put( asFloat(alpha(c)) );
	}

	public static void writeOpaqueColor(int c, FloatBuffer b) {
		b.put( asFloat(red  (c)) );
		b.put( asFloat(green(c)) );
		b.put( asFloat(blue (c)) );
	}

	public static void writeColor(int c, IntBuffer b) {
		b.put(argbToRGBA(c));
	}

	public static void writeOpaqueColor(int c, IntBuffer b) {
		b.put( (c << 8) | 0x000000ff );
	}

	public static int opaqueDistSqr(int c1, int c2) {
		int r = red(c1)   - red(c2);
		int g = green(c1) - green(c2);
		int b = blue(c1)  - blue(c2);
		return r*r + g*g + b*b;
	}

	// colorings

	public static Coloring flat(int c) {
		return new Coloring(c, c, c, c);
	}

	public static Coloring vertical(int c1, int c2) {
		return new Coloring(c1, c1, c2, c2);
	}

	public static Coloring horizontal(int c1, int c2) {
		return new Coloring(c1, c2, c2, c1);
	}

	public static Coloring corners(int tl, int tr, int br, int bl) {
		return new Coloring(tl, tr, br, bl);
	}

	public static final Coloring WHITE_COLORING = flat(WHITE);
	public static final Coloring BLACK_COLORING = flat(BLACK);

	// fields

	public final int tl;
	public final int tr;
	public final int br;
	public final int bl;

	private float[] quadFloats = null;
	private int[] quadInts = null;

	private Coloring(int tl, int tr, int br, int bl) {
		this.tl = tl;
		this.tr = tr;
		this.br = br;
		this.bl = bl;
	}

	public boolean isNone() {
		return tl == 0 && tr == 0 && br == 0 && bl == 0;
	}

	public boolean isTransparent() {
		return alpha(tl) == 0 && alpha(tr) == 0 && alpha(br) == 0 && alpha(bl) == 0;
	}

	public boolean isOpaque() {
		return alpha(tl) == 255 && alpha(tr) == 255 && alpha(br) == 255 && alpha(bl) == 255;
	}

	public boolean isVertical() {
		return tl == tr && bl == br;
	}

	public boolean isHorizontal() {
		return tl == bl && tr == br;
	}

	// s is zero to 1 - zero left
	public Coloring verticalSample(float x) {
		return Coloring.vertical(mix(tl, tr, x), mix(bl, br, x));
	}

	// s is zero to 1 - zero bottom
	public Coloring horizontalSample(float y) {
		return Coloring.horizontal(mix(bl, tl, y), mix(br, tr, y));
	}

	public Coloring opaque() {
		return isOpaque() ? this : new Coloring(Argb.opaque(tl), Argb.opaque(tr), Argb.opaque(br), Argb.opaque(bl));
	}

	public int colorSample(float x, float y) {
		int c1 = mix(tl, tr, x);
		int c2 = mix(bl, br, x);
		return   mix(c1, c2, y);
	}

	public int getColor(Corner c) {
		switch (c) {
		case BL: return bl;
		case BR: return br;
		case TR: return tr;
		case TL: return tl;
		default: throw new IllegalStateException();
		}
	}

	public float[] asQuadFloats() {
		if (quadFloats == null) {
			quadFloats = new float[16];
			writeColorAsFloats(tl, quadFloats,  0);
			writeColorAsFloats(tr, quadFloats,  4);
			writeColorAsFloats(br, quadFloats,  8);
			writeColorAsFloats(bl, quadFloats, 12);
		}
		return quadFloats;
	}

	public int[] asQuadInts() {
		return quadInts == null ? quadInts = new int[] {
				argbToRGBA(tl),
				argbToRGBA(tr),
				argbToRGBA(br),
				argbToRGBA(bl),
			} : quadInts;
	}

	public void writeQuad(Corner start, IntBuffer b) {
		switch (start) {
		case BL:
			writeColor(bl, b);
			writeColor(br, b);
			writeColor(tr, b);
			writeColor(tl, b);
			break;
			case BR:
			writeColor(br, b);
			writeColor(tr, b);
			writeColor(tl, b);
			writeColor(bl, b);
			break;
		case TR:
			writeColor(tr, b);
			writeColor(tl, b);
			writeColor(bl, b);
			writeColor(br, b);
			break;
		case TL:
			writeColor(tl, b);
			writeColor(bl, b);
			writeColor(br, b);
			writeColor(tr, b);
			break;
		}
	}

	public void writeQuadRev(Corner start, IntBuffer b) {
		switch (start) {
		case BL:
			writeColor(bl, b);
			writeColor(tl, b);
			writeColor(tr, b);
			writeColor(br, b);
			break;
			case BR:
			writeColor(br, b);
			writeColor(bl, b);
			writeColor(tl, b);
			writeColor(tr, b);
			break;
		case TR:
			writeColor(tr, b);
			writeColor(br, b);
			writeColor(bl, b);
			writeColor(tl, b);
			break;
		case TL:
			writeColor(tl, b);
			writeColor(tr, b);
			writeColor(br, b);
			writeColor(bl, b);
			break;
		}
	}

	@Override
	public String toString() {
		return String.format("0x%08x 0x%08x 0x%08x 0x%08x", tl, tr, br, bl);
	}
}
