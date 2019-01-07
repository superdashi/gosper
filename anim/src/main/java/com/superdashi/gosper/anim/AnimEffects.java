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
package com.superdashi.gosper.anim;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.color.Coloring;

public class AnimEffects {

	public static AnimEffect translate(float x1, float y1, float z1, float x2, float y2, float z2) {
		return t -> {
			float s = 1f - t;
			return AnimState.translate(
					x1 * s + x2 * t,
					y1 * s + y2 * t,
					z1 * s + z2 * t
					);
		};
	}

	public static AnimEffect scale(float s1, float s2) {
		return t -> {
			float s = s1 * (1 - t) + s2 * t;
			return AnimState.scale(s, s, s);
		};
	}

	public static AnimEffect scale(float sx1, float sy1, float sz1, float sx2, float sy2, float sz2) {
		return t -> {
			float s = 1f - t;
			return AnimState.scale(
					sx1 * s + sx2 * t,
					sy1 * s + sy2 * t,
					sz1 * s + sz2 * t
					);
		};
	}

	public static AnimEffect rotate(float rx, float ry, float rz, float a1, float a2) {
		return t -> {
			float a = a1 * (1f - t) + a2 * t;
			return AnimState.rotate(rx, ry, rz, a);
		};
	}

	public static AnimEffect rotateHue(boolean c2) {
		return t -> {
			return AnimState.rotateHue(c2, t);
		};
	}

	public static AnimEffect rotateHue(boolean c2, AnimInterpolator interpolator) {
		return t -> {
			return AnimState.rotateHue(c2, interpolator.interpolate(t));
		};
	}

	public static AnimEffect scaleSaturation(boolean c2) {
		return t -> {
			return AnimState.scaleSaturation(c2, t);
		};
	}

	public static AnimEffect scaleSaturation(boolean c2, AnimInterpolator interpolator) {
		return t -> {
			return AnimState.scaleSaturation(c2, interpolator.interpolate(t));
		};
	}

	public static AnimEffect scaleValue(boolean c2) {
		return t -> {
			return AnimState.scaleValue(c2, t);
		};
	}

	public static AnimEffect scaleValue(boolean c2, AnimInterpolator interpolator) {
		return t -> {
			return AnimState.scaleValue(c2, interpolator.interpolate(t));
		};
	}

	public static AnimEffect color(boolean c2, int color) {
		float r = Coloring.asFloat(Argb.red  (color));
		float g = Coloring.asFloat(Argb.green(color));
		float b = Coloring.asFloat(Argb.blue (color));
		return t -> {
			return AnimState.color(c2, r, g, b, t);
		};
	}
}
