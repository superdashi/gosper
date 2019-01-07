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

import java.util.ArrayList;
import java.util.List;

public abstract class AnimSequence {

	private final List<Animator> animators = new ArrayList<>();

	public boolean addAnimator(Animator animator) {
		if (animator == null) throw new IllegalArgumentException("null animator");
		if (animators.contains(animator)) return false;
		animators.add(animator);
		return true;
	}

	public boolean removeAnimator(Animator animator) {
		if (animator == null) throw new IllegalArgumentException("null animator");
		return animators.remove(animator);
	}

	public void clearAnimators() {
		animators.clear();
	}

	public void applyAtTime(long t) {
		AnimState state = AnimState.neutral();
		for (Animator animator : animators) {
			animator.applyAtTime(t, state);
		}
		apply(state);
	}

	protected abstract void apply(AnimState state);
}
