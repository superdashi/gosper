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
package com.superdashi.gosper.util;

import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.intgeom.IntRect;

public class Geometry {

	public static IntRect roundRectToIntRect(Rect rect) {
		return IntRect.bounded(Math.round(rect.minX), Math.round(rect.minY), Math.round(rect.maxX), Math.round(rect.maxY));
	}

	public static Rect intRectToRect(IntRect rect) {
		return Rect.atPoints(rect.minX, rect.minY, rect.maxX, rect.maxY);
	}

	public static Point exactCenter(IntRect rect) {
		return new Point((rect.minX + rect.maxX)*0.5f, (rect.minY + rect.maxY)*0.5f);
	}

	private Geometry() { }
}
