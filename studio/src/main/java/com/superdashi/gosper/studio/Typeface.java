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

import java.awt.Font;

import com.superdashi.gosper.layout.StyledText;
import com.tomgibara.streams.Streams;

public abstract class Typeface {

	//TODO make configurable
	private static final String SYSTEM_FONT_NAME = "sans-serif";
	// this means iota is produced from pngs, this is much slower, but handy for iteration
	private static final boolean PRODUCE_IOTA = false;

	private static int systemDefaultSize = -1;
	private static Typeface systemDefault = null;
	private static Typeface iota = null;

	private static String codepointToString(int c) {
		if (c < 65536) return String.valueOf((char) c);
		return new StringBuilder(1).appendCodePoint(c).toString();
	}

	public static Typeface ezo() {
		return EzoTypeface.instance;
	}

	public static Typeface iota() {
		if (iota == null) {
			iota = PRODUCE_IOTA ?
					AtlasProducer.iotaInstance().createTypeface() :
					AtlasTypeface.deserialize( Streams.streamInput(Typeface.class.getClassLoader().getResourceAsStream("iota.atlas")) );
		}
		return iota;
	}

	public static Typeface systemDefault(int size) {
		if (size < 1) throw new IllegalArgumentException("invalid size");
		if (systemDefaultSize != size) {
			String name = SYSTEM_FONT_NAME;
			systemDefault = new FontTypeface(
					new Font(name, Font.PLAIN                          , size),
					new Font(name,              Font.BOLD              , size),
					new Font(name,                          Font.ITALIC, size),
					new Font(name,              Font.BOLD | Font.ITALIC, size)
					);
			systemDefaultSize = size;
		}
		return systemDefault;
	}

	public static Typeface fromFont(Font font) {
		if (font == null) throw new IllegalArgumentException("null font");
		return new FontTypeface(font);
	}

	public static Typeface fromFonts(Font regular, Font bold, Font italic, Font boldItalic) {
		if (regular == null) throw new IllegalArgumentException("null regular");
		if (bold == null) throw new IllegalArgumentException("null bold");
		if (italic == null) throw new IllegalArgumentException("null italic");
		if (boldItalic == null) throw new IllegalArgumentException("null boldItalic");
		return new FontTypeface(regular, bold, italic, boldItalic);
	}

	private TypefaceMetrics metrics = null;

	Typeface() { }

	public TypefaceMetrics metrics() {
		return metrics == null ? metrics = new TypefaceMetrics(measurer()) : metrics;
	}

	abstract TextMeasurer measurer();

	abstract TextRenderer renderer(LocalCanvas canvas);

	//TODO consider returning advance
	interface TextRenderer {

		Typeface typeface();

		default void renderChar  (int   x, int   y,                  int c     ) { renderChar  (x, y, TextStyle.regular(), c); }
		default void renderChar  (float x, float y,                  int c     ) { renderChar  (x, y, TextStyle.regular(), c); }

		default void renderChar  (int   x, int   y, TextStyle style, int c     ) { renderString(x, y, style, codepointToString(c)); }
		default void renderChar  (float x, float y, TextStyle style, int c     ) { renderString(x, y, style, codepointToString(c)); }

		default void renderString(int x,   int   y,                  String str) { renderString(x, y, TextStyle.regular(), str); }
		default void renderString(float x, float y,                  String str) { renderString(x, y, TextStyle.regular(), str); }

		default void renderString(int   x, int   y, TextStyle style, String str) { renderText  (x, y, new StyledText(style.asStyle(), str) ); }
		default void renderString(float x, float y, TextStyle style, String str) { renderText  (x, y, new StyledText(style.asStyle(), str) ); }

		void renderText(int   x, int   y, StyledText text);
		void renderText(float x, float y, StyledText text);

	}

	interface TextMeasurer {

		IntFontMetrics intMetrics(TextStyle style);

		//FloatFontMetrics floatMetrics(TextStyle style);

		int accommodatedCharCount(TextStyle style, String str, int width, int ellipsisWidth);
		int accommodatedCharCount(StyledText text, int width, int ellipsisWidth);

		int intRenderedWidthOfString(TextStyle style, String str);
		//TODO want a method for styled text too

	}
}
