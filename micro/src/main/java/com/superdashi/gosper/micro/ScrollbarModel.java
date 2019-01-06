package com.superdashi.gosper.micro;

import com.tomgibara.intgeom.IntRange;

public final class ScrollbarModel extends Model {

	// fields

	final Mutations mutations;
	private IntRange range;
	private IntRange span;

	// constructors

	ScrollbarModel(ActivityContext context, Mutations mutations) {
		super(context);
		this.mutations = mutations;
		range = IntRange.ZERO_RANGE;
		span = IntRange.ZERO_RANGE;
	}

	// accessors

	public IntRange range() {
		return range;
	}

	public IntRange span() {
		return span;
	}

	public void range(IntRange range) {
		if (range == null) throw new IllegalArgumentException("null range");
		if (range.equals(this.range)) return;
		this.range = range;
		updateSpan(span);
	}

	public void range(int start, int finish) {
		if (start == range.min && finish == range.max) return; // no change
		if (finish < start) throw new IllegalArgumentException("start exceeds finish");
		range = IntRange.bounded(start, finish);
		updateSpan(span);
	}

	public void span(IntRange span) {
		if (span == null) throw new IllegalArgumentException("null span");
		if (range.equals(this.range)) return;
		updateSpan(span);
	}

	public void span(int min, int max) {
		if (min == span.min && max == span.max) return; // no change
		if (max < min) throw new IllegalArgumentException("max exceeds min");
		updateSpan(IntRange.bounded(min, max));
	}

	public void update(int min, int max, int start, int finish) {
		boolean spanSame = min == span.min && max == span.max;
		boolean rangeSame = start == range.min && finish == range.max;
		if (rangeSame && spanSame) return; // no change
		if (finish < start) throw new IllegalArgumentException("start exceeds finish");
		if (max < min) throw new IllegalArgumentException("max exceeds min");
		if (!rangeSame) range = IntRange.bounded(min, max);
		updateSpan(spanSame ? span : IntRange.bounded(start, finish));
	}

	@Override
	public String toString() {
		return "[" + range.min + " (" + span.min + ","  + span.max + ") " + range.max + "]";
	}

	// package scoped methods

	boolean matches(ScrollbarModel that) {
		return that != null && this.range.equals(that.range) && this.span.equals(that.span);
	}

	private void updateSpan(IntRange span) {
		span = span.intersection(range).get();
		if (this.span.equals(span)) return;
		this.span = span;
		mutations.count++;
		requestRedraw();
	}
}
