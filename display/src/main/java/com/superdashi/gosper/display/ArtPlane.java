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
package com.superdashi.gosper.display;

import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;

public class ArtPlane {

	public final float depth;
	// the width that occupies full screen at the plane depth
	public final float width;
	// the height that occupies full screen at the plane depth
	public final float height;
	// the rectangle that occupies full screen at the plane depth
	public final Rect rect;
	// transforms the unit square to the full screen rect at plane depth
	public final Transform trans;

	public ArtPlane(float depth, float hScale, float vScale) {
		this.depth = depth;
		this.width = depth * hScale;
		this.height = depth * vScale;
		rect = Rect.centerAtOrigin(width, height);
		trans = Transform.translateAndScale(Rect.UNIT_SQUARE, rect);
	}

}