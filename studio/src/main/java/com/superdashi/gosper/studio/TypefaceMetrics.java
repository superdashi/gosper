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

import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.studio.Typeface.TextMeasurer;

public final class TypefaceMetrics {

	private final TextMeasurer measurer;

	TypefaceMetrics(TextMeasurer measurer) {
		this.measurer = measurer;
	}

	//TODO consider convenience method that does not take style
	//TODO rename to intFontMetrics
	public IntFontMetrics fontMetrics(TextStyle style) {
		if (style == null) throw new IllegalArgumentException("null style");
		return measurer.intMetrics(style);
	}

	public int accommodatedCharCount(TextStyle style, String str, int width, int ellipsisWidth) {
		if (style == null) throw new IllegalArgumentException("null style");
		if (str == null) throw new IllegalArgumentException("null str");
		if (width < 0) throw new IllegalArgumentException("negative width");
		if (ellipsisWidth < 0) throw new IllegalArgumentException("negative ellipsisWidth");
		if (str.isEmpty()) return 0;
		return measurer.accommodatedCharCount(style, str, width, Math.min(width, ellipsisWidth));
	}

	public int accommodatedCharCount(Style style, StyledText text, int width, int ellipsisWidth) {
		if (text == null) throw new IllegalArgumentException("null text");
		if (width < 0) throw new IllegalArgumentException("negative width");
		if (text.isEmpty()) return 0;
		if (ellipsisWidth < 0) throw new IllegalArgumentException("negative ellipsisWidth");
		return measurer.accommodatedCharCount(style, text, width, Math.min(width, ellipsisWidth));
	}

	public int intRenderedWidthOfString(TextStyle style, String str) {
		if (style == null) throw new IllegalArgumentException("null style");
		if (str == null) throw new IllegalArgumentException("null str");
		if (str.isEmpty()) return 0;
		return measurer.intRenderedWidthOfString(style, str);
	}
}
