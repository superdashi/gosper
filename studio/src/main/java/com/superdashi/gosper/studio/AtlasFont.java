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
package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.superdashi.gosper.studio.Canvas.IntOps;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamBytes;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;

class AtlasFont {

	static AtlasFont deserialize(ReadStream s) {
		//TODO need a more efficient way of doing this
		int maskSize = s.readInt();
		StreamBytes bytes = Streams.bytes(maskSize, maskSize);
		s = s.to(bytes.writeStream()).transferFully().residualStream();
		BufferedImage image;
		try {
			image = ImageIO.read(bytes.readStream().asInputStream());
		} catch (IOException e) {
			throw new RuntimeException("invalid mask image", e);
		}
		ImageMask mask = new ImageMask(image);
		int underlineThickness = s.readInt();
		AtlasGlyph[] glyphs = new AtlasGlyph[s.readInt()];
		int count = s.readInt();
		for (int i = 0; i < count; i++) {
			AtlasGlyph glyph = AtlasGlyph.deserialize(s);
			glyph.cutMask(mask);
			glyphs[glyph.codepoint] = glyph;
		}
		int top = s.readInt();
		int bottom = s.readInt();

		return new AtlasFont(
				mask,
				underlineThickness,
				glyphs,
				top,
				bottom
				);
	}

	// we hold this for serialization
	private final ImageMask mask;

	// needed for rendering
	private final int underlineThickness;
	private final AtlasGlyph[] glyphs;

	// needed for metrics
	final int top;
	final int bottom;

	AtlasFont(ImageMask mask, int underlineThickness, AtlasGlyph[] glyphs, int top, int bottom) {
		this.mask = mask;
		this.underlineThickness = underlineThickness;
		this.glyphs = glyphs;
		this.top = top;
		this.bottom = bottom;
	}

	void serialize(WriteStream s) {
		//NOTE: we have to measure byte stream because sometimes ImageIO doesn't bother to read all bytes back!
		//NOTE: we write to bytes instead of measuring and writing again, because there's no guarantee that bytes will be indentical
		StreamBytes bytes = Streams.bytes();
		try {
			ImageIO.write(mask.image, "PNG", bytes.writeStream().asOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("failed to write mask image", e);
		}
		s.writeInt(bytes.length());
		s.from(bytes.readStream()).transferFully();

		s.writeInt(underlineThickness);
		s.writeInt(glyphs.length);
		int count = 0;
		for (AtlasGlyph glyph : glyphs) {
			if (glyph != null) count++;
		}
		s.writeInt(count);
		for (AtlasGlyph glyph : glyphs) {
			if (glyph != null) glyph.serialize(s);
		}

		s.writeInt(top);
		s.writeInt(bottom);
	}

	int intRenderedWidthOfString(String str) {
		switch (str.length()) {
		case 0 : {
			return 0;
		}
		case 1 : {
			int c = str.codePointAt(0);
			if (!supported(c)) return 0;
			//TODO should use style here
			return glyphs[c].maskWidth;
		}
		default: {
			int[] cps = str.codePoints().toArray();
			int first = firstSupported(cps);
			int last = lastSupported(cps);
			if (last <= first) return 0; // none supported
			if (first - last == 1) { // only one supported
				//TODO should use style here
				return glyphs[cps[first]].maskWidth;
			}
			int width = baselineWidthOfString(cps);
			//TODO should use style here
			AtlasGlyph firstGlyph = glyphs[cps[first]];
			AtlasGlyph lastGlyph = glyphs[cps[last]];
			width += firstGlyph.offset; // left overhang
			width += lastGlyph.maskWidth - lastGlyph.width - lastGlyph.offset; // right overhang
			return width;
		}
		}
	}

	int accommodatedCharCount(int[] codepoints, int width) {
		int p = -1;
		int i = 0;
		for (; i < codepoints.length; i++) {
			int c = codepoints[i];
			if (!supported(c)) continue;
			AtlasGlyph glyph = glyphs[c];
			if (p == -1) {
				width += glyph.offset; //
				if (width < 0) break; // no space for left-overhang
			} else {
				width -= kerning(p, c);
				if (width < 0) break; // no space to kern
			}
			width -= glyph.width;
			if (width < 0) break; // no space for baseline
			width -= glyph.maskWidth - glyph.width - glyph.offset;
			if (width < 0) break; // no space for right overhang
			p = c; // fits - iterate
		}
		return i;
	}

	int baselineWidthOfString(int[] codepoints) {
		assert codepoints.length > 0;
		int p = 0;
		int sum = 0;
		AtlasGlyph glyph = null;
		for (int c : codepoints) {
			if (!supported(c)) continue;
			if (p != 0) sum += kerning(p, c);
			glyph = glyphs[c];
			sum += glyph.maskWidth;
			p = c;
		}
		// adjust last - only want the baseline width
		return sum - glyph.maskWidth + glyph.width;
	}

	// -1 indicates no previous character
	// y is already baseline adjusted - ie. is above all possible font content
	int renderString(int x, int y, int underlineOffset, int p, int[] codepoints, Canvas canvas) {
		IntOps ops = canvas.intOps();
		int startX = x;
		for (int i = 0; i < codepoints.length; i++) {
			int c = codepoints[i];
			if (!supported(c)) continue;
			AtlasGlyph g = glyphs[c];
			if (p != -1) x += kerning(p, c);
			int my = y + g.offsetAbove;
			int mx = x - g.offset;
			ops.fillFrame(g.mask(), IntCoords.at(mx, my));
			x += g.maskWidth;
			p = c;
		}
		if (underlineOffset != Integer.MIN_VALUE) {
			ops.fillRect(IntRect.rectangle(startX, y + underlineOffset, x - startX, underlineThickness));
		}
		return x;
	}

	private int firstSupported(int[] codepoints) {
		int i = 0;
		for (; i < codepoints.length; i++) {
			if (supported(codepoints[i])) break;
		}
		return i;
	}

	private int lastSupported(int[] codepoints) {
		int i = codepoints.length - 1;
		for (; i >= 0; i--) {
			if (supported(codepoints[i])) break;
		}
		return i;
	}

	private boolean supported(int c) {
		return c > 0 && c < 127 && glyphs[c] != null;
	}

	private int kerning(int c1, int c2) {
		if (c1 == 32 || c2 == 32) return 0;
		AtlasGlyph g = glyphs[c1];
		// all just the default kern
		if (g.kerning == null) {
			assert g.defaultKern != Integer.MIN_VALUE;
			return g.defaultKern;
		}
		// all stored in kerning store
		if (g.defaultKern == Integer.MIN_VALUE) {
			return g.kerning.get(c2) + AtlasTypeface.MIN_KERN;
		}
		//TODO store in a more sophisticated way
		throw new UnsupportedOperationException();
	}

}
