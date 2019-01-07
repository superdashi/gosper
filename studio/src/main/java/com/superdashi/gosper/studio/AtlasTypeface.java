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

import java.util.Arrays;

import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.layout.StyledText.Segment;
import com.superdashi.gosper.studio.Canvas.State;
import com.superdashi.gosper.studio.Typeface.TextMeasurer;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

class AtlasTypeface extends Typeface implements TextMeasurer {

	// statics

	private static final int BOLD_FLAG = 1;
	private static final int ITALIC_FLAG = 2;

	/*
	private static final int REGULAR = 0;
	private static final int BOLD = BOLD_FLAG;
	private static final int ITALIC = ITALIC_FLAG;
	private static final int BOLD_ITALIC = BOLD_FLAG | ITALIC_FLAG;
	*/

	private static int styleIndex(TextStyle style) {
		return (style.bold ? BOLD_FLAG : 0) + (style.italic ? ITALIC_FLAG : 0);
	}

	// these should be adjustable
	static final int MIN_KERN = -2;
	static final int MAX_KERN = 2;

	static AtlasTypeface deserialize(ReadStream s) {
		return new AtlasTypeface(
				s.readInt(),
				s.readInt(),
				s.readInt(),
				s.readInt(),
				new AtlasFont[] {
						AtlasFont.deserialize(s),
						AtlasFont.deserialize(s),
						AtlasFont.deserialize(s),
						AtlasFont.deserialize(s)
				}
				);
	}

	// fields

	private final int baseline;
	private final int lineHeight;
	private final int ascent;
	private final int descent;
	private final AtlasFont[] fonts;
	private final IntFontMetrics[] metrics;

	// constructors

	AtlasTypeface(int baseline, int lineHeight, int ascent, int descent, AtlasFont[] fonts) {
		this.baseline = baseline;
		this.lineHeight = lineHeight;
		this.ascent = ascent;
		this.descent = descent;
		this.fonts = fonts;
		metrics = new IntFontMetrics[4]; // populate lazily
	}

	void serialize(WriteStream s) {
		s.writeInt(baseline);
		s.writeInt(lineHeight);
		s.writeInt(ascent);
		s.writeInt(descent);
		Arrays.stream(fonts).forEach(f -> f.serialize(s));
	}

	// typeface methods

	@Override
	TextMeasurer measurer() {
		return this;
	}

	@Override
	TextRenderer renderer(LocalCanvas canvas) {
		return new AtlasRenderer(canvas);
	}

	// measurer methods

	@Override
	public IntFontMetrics intMetrics(TextStyle style) {
		if (style == null) throw new IllegalArgumentException("null style");
		int i = styleIndex(style);
		IntFontMetrics m = metrics[i];
		if (m == null) {
			AtlasFont f = fonts[i];
			int leading = lineHeight - f.top - f.bottom;
			m = new IntFontMetrics(baseline, f.top, f.bottom, leading, ascent, descent);
			metrics[i] = m;
		}
		return m;
	}

	@Override
	public int accommodatedCharCount(TextStyle style, String str, int width, int ellipsisWidth) {
		if (style == null) throw new IllegalArgumentException("null style");
		if (str == null) throw new IllegalArgumentException("null str");
		if (ellipsisWidth < 0L) throw new IllegalArgumentException("negative ellipsisWidth");
		AtlasFont font = font(style);
		int[] codepoints = str.codePoints().toArray();
		return accommodatedCharCount(str.length(), w -> font.accommodatedCharCount(codepoints, w), width, ellipsisWidth);
	}

	@Override
	public int accommodatedCharCount(StyledText text, int width, int ellipsisWidth) {
		if (text == null) throw new IllegalArgumentException("null text");
		if (ellipsisWidth < 0L) throw new IllegalArgumentException("negative ellipsisWidth");
		Iterable<Segment> segments = text.segments();
		return accommodatedCharCount(text.length(), w -> accommodatedCharCount(segments, w), width, ellipsisWidth);
	}

	@Override
	public int intRenderedWidthOfString(TextStyle style, String str) {
		if (style == null) throw new IllegalArgumentException("null style");
		if (str == null) throw new IllegalArgumentException("null str");
		return font(style).intRenderedWidthOfString(str);
	}

	// private methods

	private AtlasFont font(TextStyle style) {
		return fonts[styleIndex(style)];
	}

	private <T> int accommodatedCharCount(int length, Fitter fitter, int width, int ellipsisWidth) {
		if (width < 0) return 0; // no fits possible
		int count = fitter.fit(width);
		if (count == length) return count; // all fits
		if (ellipsisWidth > width) return 0; // not even ellipsis will fit
		return fitter.fit(width - ellipsisWidth);
	}

	private int accommodatedCharCount(Iterable<Segment> segments, int width) {
		int total = 0;
		for (Segment segment : segments) {
			int length = segment.to - segment.from;
			final String str = segment.text.toString();
			int[] codepoints = str.codePoints().toArray();
			TextStyle style = TextStyle.fromStyle(segment.style);
			AtlasFont font = font(style);
			int count = font.accommodatedCharCount(codepoints, width);
			total += count;
			if (count < length) return total;
			width -= font.baselineWidthOfString(codepoints);
		}
		return total;
	}

	//TODO 2 should be configurable
	private int underlineOffset(TextStyle style) {
		return style.underlined ? baseline + 2 : Integer.MIN_VALUE;
	}

	// inner classes

	private class AtlasRenderer implements TextRenderer {

		private final Canvas canvas;

		AtlasRenderer(Canvas canvas) {
			this.canvas = canvas;
		}

		@Override
		public Typeface typeface() {
			return AtlasTypeface.this;
		}

		@Override
		public void renderString(int x, int y, TextStyle style, String str) {
			if (style == null) throw new IllegalArgumentException("null style");
			if (str == null) throw new IllegalArgumentException("null str");
			if (str.isEmpty()) return;
			int[] codepoints = str.codePoints().toArray();
			font(style).renderString(x, y - baseline, underlineOffset(style), -1, codepoints, canvas);
		}

		@Override
		public void renderText(int x, int y, StyledText text) {
			y -= baseline;
			//TODO is there a more efficient way of handling color changes?
			State state = null;
			int p = -1;
			for (Segment segment : text.segments()) {
				// skip empty segments
				if (segment.text.length() == 0) continue;
				int color = segment.style.colorFg();
				//TODO need a proper way to indicate color set on style
				if (color != 0) {
					if (state == null) state = canvas.recordState();
					canvas.color(color);
				} else {
					if (state != null) {
						state.restore();
						state = null;
					}
				}
				TextStyle style = TextStyle.fromStyle(segment.style);
				int[] codepoints = segment.text.codePoints().toArray();
				x = font(style).renderString(x, y, underlineOffset(style), p, codepoints, canvas);
				p = codepoints[codepoints.length - 1];
			}
			if (state != null) state.restore();
		}

		@Override
		public void renderText(float x, float y, StyledText text) {
			renderText(Math.round(x), Math.round(y), text);
		}

	}

	private interface Fitter {

		int fit(int width);

	}

}
