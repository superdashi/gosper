package com.superdashi.gosper.micro;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public final class Constraints {

	// statics

	static void checkBounds(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (bounds.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
	}

	// fields

	private IntRect bounds;
	private Map<Location, IntDimensions> minimums;

	// constructors

	public Constraints(IntRect bounds) {
		this.bounds = bounds;
	}

	// public accessors

	public IntRect bounds() {
		return bounds;
	}

	public void bounds(IntRect bounds) {
		checkBounds(bounds);
		this.bounds = bounds;
	}

	// public methods

	public void setMinimumContentSize(Location location, IntDimensions minimumSize) {
		if (location == null) throw new IllegalArgumentException("null location");
		if (minimumSize == null) throw new IllegalArgumentException("null minimumSize");
		if (minimumSize.isNothing()) {
			if (minimums != null) minimums.remove(location);
		} else {
			if (minimums == null) minimums = new HashMap<>();
			minimums.put(location, minimumSize);
		}
	}

	public IntDimensions getMinimumSize(Location location) {
		if (minimums == null) return IntDimensions.NOTHING;
		IntDimensions minimumSize = minimums.get(location);
		return minimumSize == null ? IntDimensions.NOTHING : minimumSize;
	}

	// object methods

	@Override
	public int hashCode() {
		return bounds.hashCode() + Objects.hashCode(minimums);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Constraints)) return false;
		Constraints that = (Constraints) obj;
		return this.bounds.equals(that.bounds) && Objects.equals(this.minimums, that.minimums);
	}

	@Override
	public String toString() {
		return "[Constraints bounds=" + bounds + ", minimums=" + (minimums==null ? Collections.emptyMap() : minimums) + "]";
	}

}
