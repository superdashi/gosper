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
