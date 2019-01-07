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

public final class Animation {

	private final AnimEffect effect;
	private final AnimInterpolator interpolator;

	Animation(AnimEffect effect, AnimInterpolator interpolator) {
		this.effect = effect;
		this.interpolator = interpolator;
	}

	public void applyAtTime(float t, AnimState state) {
		AnimState s = effect.stateAtTime(interpolator.interpolate(t));
		state.apply(s);
	}

	public Animator animator(long startTime, long finishTime, Terminal startTerminal, Terminal finishTerminal) {
		if (startTime > finishTime) throw new IllegalArgumentException("finishTime preceeds startTime");
		if (startTerminal == null) throw new IllegalArgumentException("null startTerminal");
		if (finishTerminal == null) throw new IllegalArgumentException("null finishTerminal");
		return new Animator(this, startTime, finishTime, startTerminal, finishTerminal);
	}

}
