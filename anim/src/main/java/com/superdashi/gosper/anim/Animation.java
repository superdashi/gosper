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
