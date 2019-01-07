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

public class AnimInterpolators {

	public static AnimInterpolator linear() {
		return t -> t;
	}

	public static AnimInterpolator quadratic() {
		return t -> t * t;
	}

	public static AnimInterpolator cubic() {
		return t -> t * t * t;
	}

	public static AnimInterpolator accelDecel() {
		return t -> 1f - ((float) Math.cos(t * Math.PI) + 1f) * 0.5f;
	}

	public static AnimInterpolator reverse() {
		return t -> 1f - t;
	}

	public static AnimInterpolator bounce() {
		return t -> t < 0.5f ? 2f * t : 2f * (1f - t);
	}

	public static AnimInterpolator cycle() {
		return t -> 1f - ((float) Math.cos(t * (2.0 * Math.PI)) + 1f) * 0.5f;
	}

	public static AnimInterpolator repeat(int reps) {
		if (reps < 1) throw new IllegalArgumentException("invalid reps");
		return reps == 1 ? linear() : t -> t * reps % 1f;
	}
}
