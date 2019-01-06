package com.superdashi.gosper.micro;

import com.superdashi.gosper.layout.Style;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public final class Place {

	public final Location location;
	public final IntRect outerBounds;
	public final Style style;
	public final IntRect innerBounds;
	public final IntDimensions innerDimensions;

	Place(Location location, IntRect outerBounds, Style style) {
		assert !style.isMutable();
		assert outerBounds != null;
		this.location = location;
		this.outerBounds = outerBounds;
		this.style = style.noMargins();
		innerBounds = outerBounds.minus(style.margins());
		innerDimensions = innerBounds.dimensions();
	}

	@Override
	public String toString() {
		return location + ":" + outerBounds;
	}
}
