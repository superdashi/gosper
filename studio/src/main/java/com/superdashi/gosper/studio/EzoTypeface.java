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

import static java.lang.Math.round;

import java.util.List;
import java.util.stream.Collectors;

import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.layout.StyledText.Segment;
import com.superdashi.gosper.studio.Canvas.IntOps;
import com.superdashi.gosper.studio.Canvas.State;
import com.tomgibara.ezo.Ezo;
import com.tomgibara.ezo.Ezo.Renderer;

final class EzoTypeface extends Typeface {

	private static final Ezo[] ezos = new Ezo[8];
	private static final EzoMeasurer measurer = new EzoMeasurer();
	private static final IntFontMetrics metrics = new IntFontMetrics(6, 6, 2, 1, 5, 2);

	static final EzoTypeface instance = new EzoTypeface();

	private static Ezo ezo(TextStyle style) {
		Ezo ezo = ezos[style.index];
		if (ezo == null) {
			ezo = Ezo.regular()
				.withBold(style.bold)
				.withItalic(style.italic)
				.withUnderline(style.underlined);
			ezos[style.index] = ezo;
		}
		return ezo;
	}

	private static boolean flag(int value) {
		return value != Style.NO_VALUE && value > 0;
	}

	private static Ezo ezo(Style style) {
		return Ezo.regular()
			.withBold(flag(style.textWeight()))
			.withItalic(flag(style.textItalic()))
			.withUnderline(flag(style.textUnderline()));
	}

	private EzoTypeface() { }

	@Override
	TextRenderer renderer(LocalCanvas canvas) {
		return new EzoRenderer(canvas);
	}

	@Override
	TextMeasurer measurer() {
		return measurer;
	}

	private static class EzoRenderer implements TextRenderer {

		private final IntOps ops;
		private Renderer regular;

		private EzoRenderer(Canvas canvas) {
			this.ops = canvas.intOps();
		}

		@Override
		public Typeface typeface() {
			return instance;
		}

		@Override
		public void renderChar(int x, int y, int c) {
			regular().locate(x, y).renderChar(c);
		}

		@Override
		public void renderChar(float x, float y, int c) {
			renderChar(round(x), round(y), c);
		}

		@Override
		public void renderChar(int x, int y, TextStyle style, int c) {
			renderer(style).locate(x, y).renderChar(c);
		}

		@Override
		public void renderChar(float x, float y, TextStyle style, int c) {
			renderChar(round(x), round(y), style, c);
		}

		@Override
		public void renderString(int x, int y, String str) {
			regular().locate(round(x), round(y)).renderString(str);
		}

		@Override
		public void renderString(float x, float y, String str) {
			renderString(round(x), round(y), str);
		}

		@Override
		public void renderString(int x, int y, TextStyle style, String str) {
			renderer(style).locate(x, y).renderString(str);
		}

		@Override
		public void renderString(float x, float y, TextStyle style, String str) {
			renderString(round(x), round(y), style, str);
		}

		@Override
		public void renderText(int x, int y, StyledText text) {
			//TODO is there a more efficient way of handling color changes?
			State state = null;
			for (Segment segment : text.segments()) {
				int color = segment.style.colorFg();
				if (color != 0) {
					Canvas canvas = ops.canvas();
					if (state == null) state = canvas.recordState();
					canvas.color(color);
				} else {
					if (state != null) {
						state.restore();
						state = null;
					}
				}
				Renderer renderer = renderer(ezo(segment.style));
				renderer.locate(x, y).renderString(segment.text.toString());
				x = renderer.x();
				y = renderer.y();
			}
			if (state != null) state.restore();
		}

		@Override
		public void renderText(float x, float y, StyledText text) {
			renderText(round(x), round(y), text);
		}

		private Renderer renderer(TextStyle style) {
			return style.isRegular() ? regular() : renderer(ezo(style));
		}

		private Renderer renderer(Ezo ezo) {
			return ezo.renderer(ops::plotPixel);
		}

		private Renderer regular() {
			return regular == null ? regular = renderer(Ezo.regular()) : regular;
		}
	}

	private static class EzoMeasurer implements TextMeasurer {

		@Override
		public IntFontMetrics intMetrics(TextStyle style) {
			return metrics;
		}

		@Override
		public int accommodatedCharCount(TextStyle style, String str, int width, int ellipsisWidth) {
			return ezo(style).accommodatedCharCount(str, width, ellipsisWidth);
		}

		@Override
		public int accommodatedCharCount(StyledText text, int width, int ellipsisWidth) {
			List<Segment> segments = text.segmentStream().collect(Collectors.toList());
			int count = accommodatedCharCount(segments, width);
			if (count == text.length()) return count;
			return accommodatedCharCount(segments, width - ellipsisWidth);
		}

		@Override
		public int intRenderedWidthOfString(TextStyle style, String str) {
			return ezo(style).renderedWidthOfString(str);
		}

		private int accommodatedCharCount(List<Segment> segments, int width) {
			int total = 0;
			for (Segment segment : segments) {
				int length = segment.to - segment.from;
				Ezo ezo = ezo(segment.style);
				String str = segment.text.toString();
				int count = ezo.accommodatedCharCount(str, width, 0);
				total += count;
				if (count < length) return total;
				width -= ezo.baselineWidthOfString(str);
			}
			return total;
		}
	}

}
