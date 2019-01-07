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

public class Animator {

	public enum Terminal {
		NONE,
		HOLD,
		LOOP,
		BOUNCE;
	}

	public final Animation animation;
	public final long startTime;
	public final long finishTime;
	public final Terminal startTerminal;
	public final Terminal finishTerminal;

	final long duration;
	final float denominator;

	Animator(Animation animation, long startTime, long finishTime, Terminal startTerminal, Terminal finishTerminal) {
		this.animation = animation;

		this.startTime = startTime;
		this.finishTime = finishTime;
		this.startTerminal = startTerminal;
		this.finishTerminal = finishTerminal;

		duration = finishTime - startTime + 1;
		denominator = 1f / (finishTime - startTime);
	}

	public void applyAtTime(long t, AnimState state) {
		if (t < startTime) {
			switch (startTerminal) {
			case NONE: return;
			case HOLD: applyAtTime(0f, state); return;
			case LOOP: t = ((t - startTime) % duration) + duration;
			case BOUNCE:
				t = (t - startTime) % (2*duration);
				t = t < -duration ? t + 2*duration : -t;
			}
		} else if (t > finishTime) {
			switch (finishTerminal) {
			case NONE: return;
			case HOLD: applyAtTime(1f, state); return;
			case LOOP: t = (t - finishTime) % duration;
			case BOUNCE:
				t = (t - finishTime) % (2 * duration);
				t = t < duration ? duration - t : t - duration;
			}
		} else {
			t -= startTime;
		}
		applyAtTime(t * denominator, state);
	}

	private void applyAtTime(float t, AnimState state) {
		animation.applyAtTime(t, state);
	}
}
