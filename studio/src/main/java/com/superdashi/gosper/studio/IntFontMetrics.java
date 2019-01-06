package com.superdashi.gosper.studio;

public final class IntFontMetrics {

	public static IntFontMetrics definedBy(int baseline, int top, int bottom, int leading, int ascent, int descent) {
		// invariant: all positive
		if (baseline < 0) throw new IllegalArgumentException("negative baseline");
		if (top < 0L) throw new IllegalArgumentException("negative top");
		if (bottom < 0L) throw new IllegalArgumentException("negative bottom");
		if (leading < 0L) throw new IllegalArgumentException("negative leading");
		if (ascent < 0L) throw new IllegalArgumentException("negative ascent");
		if (descent < 0L) throw new IllegalArgumentException("negative descent");
		// invariant: top >= ascent
		if (ascent > top) throw new IllegalArgumentException("invalid ascent");
		// invariant: bottom >= descent
		if (descent > bottom) throw new IllegalArgumentException("invalid descent");

		return new IntFontMetrics(baseline, top, bottom, leading, ascent, descent);
	}

	// canonical

	// the hard-top of the font from the baseline - no glyph can extend beyond this
	public final int baseline;
	// the greatest number of pixels that any glyph extends above the baseline
	public final int top;
	// the greatest number of pixels that any glyph extends below the baseline
	public final int bottom;
	// line spacing added between the bottom of the text and the
	public final int leading;

	// derived

	public final int lineHeight;

	// stylistic

	// stylistically, typically the number of pixels that majuscules extend above the basline
	public final int ascent;
	// stylistically, the typical number of pixels that a descender extends below the baseline
	public final int descent;

	IntFontMetrics(int baseline, int top, int bottom, int leading, int ascent, int descent) {
		this.baseline = baseline;
		this.top = top;
		this.bottom = bottom;
		this.leading = leading;
		this.ascent = ascent;
		this.descent = descent;
		// derived
		lineHeight = top + bottom + leading;
	}
}
