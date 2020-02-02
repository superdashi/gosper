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
package com.superdashi.gosper.layout;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.logging.Logger;
import com.tomgibara.intgeom.IntMargins;

import java.util.Objects;

public final class Style {

	private static final Style noStyle = new Style().immutable();

	private static int combine(int under, int over) {
		return over == NO_VALUE ? under : over;
	}

	private static Alignment combine(Alignment under, Alignment over) {
		return over == null ? under : over;
	}

	private static int valueOrZero(int value) {
		return value == NO_VALUE ? 0 : value;
	}

	public static final int NO_VALUE = Integer.MIN_VALUE;
	public static Style noStyle() { return noStyle; }

//	public static int defaultedValue(int styleValue, int defaultValue) {
//		return styleValue == NO_VALUE ? defaultValue : styleValue;
//	}

	private final boolean mutable;

	private int colorFg;
	private int colorBg;

	private int marginLeft;
	private int marginTop;
	private int marginRight;
	private int marginBottom;

	private int textUnderline;
	private int textWeight;
	private int textItalic;
	private int textOutline;

	private int lineLimit; //TODO maxHeight instead?
	private int lineSpace; // as a delta to standard line space

	private Alignment alignmentX;
	private Alignment alignmentY;

	public Style() {
		mutable = true;
		colorFg = 0;
		colorBg = 0;
		marginLeft = NO_VALUE;
		marginTop = NO_VALUE;
		marginRight = NO_VALUE;
		marginLeft = NO_VALUE;
		textUnderline = NO_VALUE;
		textWeight = NO_VALUE;
		textItalic = NO_VALUE;
		textOutline = NO_VALUE;
		lineLimit = NO_VALUE;
		lineSpace = NO_VALUE;
		alignmentX = null;
		alignmentY = null;
	}

	public Style(IntMargins margins) {
		this();
		if (margins == null) throw new IllegalArgumentException("null margins");
		marginLeft = - margins.minX;
		marginRight = margins.maxX;
		marginTop = - margins.minY;
		marginBottom = margins.maxY;
	}

	private Style(Style that, boolean mutable) {
		this(that, mutable, false);
	}

	private Style(Style that, boolean mutable, boolean zeroMargins) {
		this.mutable = mutable;
		this.colorFg = that.colorFg;
		this.colorBg = that.colorBg;
		if (zeroMargins) {
			marginLeft   = 0;
			marginTop    = 0;
			marginRight  = 0;
			marginBottom = 0;
		} else {
			this.marginLeft = that.marginLeft;
			this.marginTop = that.marginTop;
			this.marginRight = that.marginRight;
			this.marginBottom = that.marginBottom;
		}
		this.textUnderline = that.textUnderline;
		this.textWeight = that.textWeight;
		this.textItalic = that.textItalic;
		this.textOutline = that.textOutline;
		this.lineLimit = that.lineLimit;
		this.lineSpace = that.lineSpace;
		this.alignmentX = that.alignmentX;
		this.alignmentY = that.alignmentY;
	}

	// accessors

	public int colorFg() {
		return colorFg;
	}

	public Style colorFg(int colorFg) {
		checkMutable();
		this.colorFg = colorFg;
		return this;
	}

	public int colorBg() {
		return colorBg;
	}

	public Style colorBg(int colorBg) {
		checkMutable();
		this.colorBg = colorBg;
		return this;
	}

	public Style margin(int margin) {
		checkMutable();
		this.marginLeft = margin;
		this.marginRight = margin;
		this.marginTop = margin;
		this.marginBottom = margin;
		return this;
	}

	public int marginLeft() {
		return marginLeft;
	}

	public Style marginLeft(int marginLeft) {
		checkMutable();
		this.marginLeft = marginLeft;
		return this;
	}

	public int marginTop() {
		return marginTop;
	}

	public Style marginTop(int marginTop) {
		checkMutable();
		this.marginTop = marginTop;
		return this;
	}

	public int marginRight() {
		return marginRight;
	}

	public Style marginRight(int marginRight) {
		checkMutable();
		this.marginRight = marginRight;
		return this;
	}

	public int marginBottom() {
		return marginBottom;
	}

	public Style marginBottom(int marginBottom) {
		checkMutable();
		this.marginBottom = marginBottom;
		return this;
	}

	public int textUnderline() {
		return textUnderline;
	}

	public Style textUnderline(int textUnderline) {
		checkMutable();
		this.textUnderline = textUnderline;
		return this;
	}

	public int textWeight() {
		return textWeight;
	}

	public Style textWeight(int textWeight) {
		checkMutable();
		this.textWeight = textWeight;
		return this;
	}

	public int textItalic() {
		return textItalic;
	}

	public Style textItalic(int textItalic) {
		checkMutable();
		this.textItalic = textItalic;
		return this;
	}

	public int textOutline() {
		return textOutline;
	}

	public Style textOutline(int textOutline) {
		checkMutable();
		this.textOutline = textOutline;
		return this;
	}

	public int lineLimit() {
		return lineLimit;
	}

	public Style lineLimit(int lineLimit) {
		checkMutable();
		this.lineLimit = lineLimit;
		return this;
	}

	public int lineSpace() {
		return lineSpace;
	}

	public Style lineSpace(int lineSpace) {
		checkMutable();
		this.lineSpace = lineSpace;
		return this;
	}

	public Alignment alignmentX() {
		return alignmentX;
	}

	public Style alignmentX(Alignment alignmentX) {
		checkMutable();
		this.alignmentX = alignmentX;
		return this;
	}

	public Alignment alignmentY() {
		return alignmentY;
	}

	public Style alignmentY(Alignment alignmentY) {
		checkMutable();
		this.alignmentY = alignmentY;
		return this;
	}

	// methods

	public Style apply(Style that) {
		checkMutable();
		if (that != noStyle) {
			this.colorFg = Argb.srcOver(that.colorFg, this.colorFg);
			this.colorBg = Argb.srcOver(that.colorBg, this.colorBg);
			this.marginLeft    = combine(this.marginLeft,    that.marginLeft   );
			this.marginTop     = combine(this.marginTop,     that.marginTop    );
			this.marginRight   = combine(this.marginRight,   that.marginRight  );
			this.marginBottom  = combine(this.marginBottom,  that.marginBottom );
			this.textUnderline = combine(this.textUnderline, that.textUnderline);
			this.textWeight    = combine(this.textWeight,    that.textWeight   );
			this.textItalic    = combine(this.textItalic,    that.textItalic   );
			this.textOutline   = combine(this.textOutline,   that.textOutline  );
			this.lineLimit     = combine(this.lineLimit,     that.lineLimit    );
			this.lineSpace     = combine(this.lineSpace,     that.lineSpace    );
			this.alignmentX    = combine(this.alignmentX,    that.alignmentX   );
			this.alignmentY    = combine(this.alignmentY,    that.alignmentY   );
		}
		return this;
	}

	// mutability

	public boolean isMutable() {
		return mutable;
	}

	public Style immutable() {
		return mutable ? new Style(this, false) : this;
	}

	public Style mutable() {
		return mutable ? this : new Style(this, true);
	}

	public Style mutableCopy() {
		return new Style(this, true);
	}

	// previously package scoped methods

	public IntMargins margins() {
		return IntMargins.widths(valueOrZero(marginLeft), valueOrZero(marginRight), valueOrZero(marginTop), valueOrZero(marginBottom));
	}

	public Style noMargins() {
		return new Style(this, true, true);
	}

	public void debug(Logger logger) {
		logger.debug().message("colorFg       {}").values(colorFg      ).log();
		logger.debug().message("colorBg       {}").values(colorBg      ).log();
		logger.debug().message("marginLeft    {}").values(marginLeft   ).log();
		logger.debug().message("marginTop     {}").values(marginTop    ).log();
		logger.debug().message("marginRight   {}").values(marginRight  ).log();
		logger.debug().message("marginBottom  {}").values(marginBottom ).log();
		logger.debug().message("textUnderline {}").values(textUnderline).log();
		logger.debug().message("textWeight    {}").values(textWeight   ).log();
		logger.debug().message("textItalic    {}").values(textItalic   ).log();
		logger.debug().message("textOutline   {}").values(textOutline  ).log();
		logger.debug().message("lineLimit     {}").values(lineLimit    ).log();
		logger.debug().message("lineSpace     {}").values(lineSpace    ).log();
		logger.debug().message("alignmentX    {}").values(alignmentX   ).log();
		logger.debug().message("alignmentY    {}").values(alignmentY   ).log();
	}

	// object methods

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Style)) return false;
		Style style = (Style) obj;
		return
				colorFg == style.colorFg &&
				colorBg == style.colorBg &&
				marginLeft == style.marginLeft &&
				marginTop == style.marginTop &&
				marginRight == style.marginRight &&
				marginBottom == style.marginBottom &&
				textUnderline == style.textUnderline &&
				textWeight == style.textWeight &&
				textItalic == style.textItalic &&
				textOutline == style.textOutline &&
				lineLimit == style.lineLimit &&
				lineSpace == style.lineSpace &&
				Objects.equals(alignmentX, style.alignmentX) &&
				Objects.equals(alignmentY, style.alignmentY);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				colorFg,
				colorBg,
				marginLeft,
				marginTop,
				marginRight,
				marginBottom,
				textUnderline,
				textWeight,
				textItalic,
				textOutline,
				lineLimit,
				lineSpace,
				alignmentX,
				alignmentY
		);
	}


	// private helper methods

	private void checkMutable() {
		if (!mutable) throw new IllegalStateException("immutable");
	}

}