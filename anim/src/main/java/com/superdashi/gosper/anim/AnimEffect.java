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

import com.superdashi.gosper.anim.Animator.Terminal;

@FunctionalInterface
public interface AnimEffect {

	AnimState stateAtTime(float t);

	default Animation interpolate() {
		return new Animation(this, AnimInterpolators.linear());
	}

	default Animation interpolate(AnimInterpolator interpolator) {
		if (interpolator == null) throw new IllegalArgumentException("null interpolator");
		return new Animation(this, interpolator);
	}

	default Animator at(long time) {
		return interpolate().animator(time, time, Terminal.HOLD, Terminal.HOLD);
	}
}
