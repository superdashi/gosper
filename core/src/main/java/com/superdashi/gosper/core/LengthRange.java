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
package com.superdashi.gosper.core;

import com.superdashi.gosper.config.Config.LengthConfig;

public final class LengthRange {

	public final float min;
	public final float max;
	public final float def;

	public LengthRange(float min, float max, float def) {
		if (min > max) throw new IllegalArgumentException("min exceeds max");
		if (def > max) throw new IllegalArgumentException("def exceeds max");
		if (min > def) throw new IllegalArgumentException("min exceeds def");
		this.min = min;
		this.max = max;
		this.def = def;
	}

	public float lengthOf(LengthConfig style) {
		float length = style.asFloat();
		if (length < min) return min;
		if (length > max) return max;
		return length;
	}

}
