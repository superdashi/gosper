package com.superdashi.gosper.core;

import java.awt.Rectangle;
import java.util.Optional;

import com.tomgibara.geom.core.Rect;
import com.tomgibara.intgeom.IntRect;

public final class Resolution {

	private static Resolution UNLIMITED = new Resolution(-1, -1);

	public static Resolution unlimited() {
		return UNLIMITED;
	}

	public static Resolution sized(int horizontal, int vertical) {
		if (horizontal < 1) throw new IllegalArgumentException("invalid h");
		if (vertical < 1) throw new IllegalArgumentException("invalid v");
		return new Resolution(horizontal, vertical);
	}

	public static Resolution ofRect(IntRect rect) {
		if (rect == null) throw new IllegalArgumentException("null rect");
		return new Resolution(rect.width(), rect.height());
	}

	public final int h;
	public final int v;

	private Resolution(int horizontal, int vertical) {
		this.h = horizontal;
		this.v = vertical;
	}

	public boolean isUnlimited() {
		return this == UNLIMITED;
	}

	public Optional<Rectangle> toRectangle() {
		return isUnlimited() ? Optional.empty() : Optional.of( new Rectangle(0, 0, h, v) );
	}

	public Optional<Rect> toRect() {
		return isUnlimited() ? Optional.empty() : Optional.of( Rect.atOrigin(h, v) );
	}

	public Optional<IntRect> toIntRect() {
		return isUnlimited() ? Optional.empty() : Optional.of( IntRect.atOrigin(h, v) );
	}

	public boolean isUnder(Resolution that) {
		if (this.isUnlimited()) return false;
		if (that.isUnlimited()) return true;
		return this.h <= that.h && this.v <= that.v;
	}

	public Resolution constrainedBy(Resolution that) {
		if (this == that) return this;
		if (this.isUnder(that)) return this;
		if (that.isUnder(this)) return that;
		return new Resolution(Math.min(this.h, that.h), Math.min(this.v, that.v));
	}

	public Resolution accommodating(Resolution that) {
		if (this == that) return this;
		if (this.isUnder(that)) return that;
		if (that.isUnder(this)) return this;
		return new Resolution(Math.max(this.h, that.h), Math.max(this.v, that.v));
	}

	public Resolution potUpTo() {
		if (isUnlimited()) return this;
		int h = Integer.highestOneBit(this.h);
		int v = Integer.highestOneBit(this.v);
		return newRes(h, v);
	}

	public Resolution potDownTo() {
		if (isUnlimited()) return this;
		int h = Integer.highestOneBit(this.h);
		int v = Integer.highestOneBit(this.v);
		if (h < this.h) h <<= 1;
		if (v < this.v) v <<= 1;
		return newRes(h, v);
	}

	public float aspectRatio() {
		return h / (float) v;
	}

	@Override
	public int hashCode() {
		return h + 31 * v;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Resolution)) return false;
		Resolution that = (Resolution) obj;
		return this.h == that.h && this.v == that.v;
	}

	@Override
	public String toString() {
		return "h=" + h + ",v=" + v;
	}

	private Resolution newRes(int h, int v) {
		return this.h == h && this.v == v ? this : new Resolution(h, v);
	}
}