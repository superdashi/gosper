package com.superdashi.gosper.studio;

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

	public int accommodatedCharCount(StyledText text, int width, int ellipsisWidth) {
		if (text == null) throw new IllegalArgumentException("null text");
		if (width < 0) throw new IllegalArgumentException("negative width");
		if (text.isEmpty()) return 0;
		if (ellipsisWidth < 0) throw new IllegalArgumentException("negative ellipsisWidth");
		return measurer.accommodatedCharCount(text, width, Math.min(width, ellipsisWidth));
	}

	public int intRenderedWidthOfString(TextStyle style, String str) {
		if (style == null) throw new IllegalArgumentException("null style");
		if (str == null) throw new IllegalArgumentException("null str");
		if (str.isEmpty()) return 0;
		return measurer.intRenderedWidthOfString(style, str);
	}
}
