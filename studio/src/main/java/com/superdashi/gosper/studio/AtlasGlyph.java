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

import com.tomgibara.intgeom.IntRect;
import com.tomgibara.storage.Store;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

final class AtlasGlyph {

	static AtlasGlyph deserialize(ReadStream s) {
		AtlasGlyph glyph = new AtlasGlyph(
				s.readInt(),
				s.readInt(),
				s.readInt(),
				s.readInt(),
				s.readInt(),
				s.readInt(),
				s.readInt(),
				s.readInt(),
				s.readInt()
				);
		glyph.defaultKern = s.readInt();
		glyph.kerning = AtlasProducer.deserializeKerning(s);
		return glyph;
	}

	final int codepoint;
	final int offset; // horizontally to mask x coord
	final int width; // baseline width
	final int offsetAbove; // distance between above mask
	final int offsetBelow; // distance below mask
	final int maskLeft;
	final int maskTop;
	final int maskWidth;
	final int maskHeight;

	private Mask mask;

	int defaultKern;
	Store<Integer> kerning;

	AtlasGlyph(int codepoint, int offset, int width, int offsetAbove, int offsetBelow, int maskLeft, int maskTop, int maskWidth, int maskHeight) {
		this.codepoint = codepoint;
		this.offset = offset;
		this.width = width;
		this.offsetAbove = offsetAbove;
		this.offsetBelow = offsetBelow;
		this.maskTop = maskTop;
		this.maskLeft = maskLeft;
		this.maskWidth = maskWidth;
		this.maskHeight = maskHeight;
		assert maskHeight > 0;
		assert maskWidth > 0;
	}

	void serialize(WriteStream s) {
		s.writeInt(codepoint);
		s.writeInt(offset);
		s.writeInt(width);
		s.writeInt(offsetAbove);
		s.writeInt(offsetBelow);
		s.writeInt(maskLeft);
		s.writeInt(maskTop);
		s.writeInt(maskWidth);
		s.writeInt(maskHeight);

		s.writeInt(defaultKern);
		AtlasProducer.serializeKerning(s, kerning);
	}

	void cutMask(Mask mask) {
		IntRect bounds = IntRect.rectangle(maskLeft, maskTop, maskWidth, maskHeight);
		if (codepoint == 32) {
			this.mask = Mask.empty(bounds.dimensions());
		} else {
			this.mask = mask.view(bounds);
		}
	}

	Mask mask() {
		return mask;
	}

}