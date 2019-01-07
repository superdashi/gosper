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
